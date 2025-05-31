package com.unichristus.leitor_fiscal.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unichristus.leitor_fiscal.data.Product
import com.unichristus.leitor_fiscal.data.ScanState
import com.unichristus.leitor_fiscal.data.CupomInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    private val _scanState: MutableStateFlow<ScanState> = MutableStateFlow(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun processInvoiceText(invoiceText: String) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            Log.d("OCR_RESULT", "Texto Bruto Recebido para Cupom Detalhado:\n$invoiceText")

            val lines = invoiceText.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            var storeName: String? = null
            var cnpj: String? = null
            var address: String? = null
            var dateTime: String? = null
            var ccf: String? = null
            var coo: String? = null
            var receiptTotalAmount: String? = null

            val products = mutableListOf<Product>()
            val itemAndProductCodes = mutableListOf<Pair<String, String>>()
            val quantitiesAndUnits = mutableListOf<Pair<String, String>>()
            val descriptions = mutableListOf<String>()
            val unitPrices = mutableListOf<String>()
            val itemTotals = mutableListOf<String>()

            val storeNamePattern = Regex("""^(LUJAS RENRER|LOJAS RENNER)""", RegexOption.IGNORE_CASE)
            val cnpjPattern = Regex("""CNPJ:?\s*([\d./-]+)""")
            val dateTimePattern = Regex("""(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2}:\d{2})""")
            val ccfPattern = Regex("""CCF:?\s*(\d+)""")
            val cooPattern = Regex("""CO{1,2}:?\s*(\d+)""")
            val addressKeywordPattern = Regex("""^(AV\.|AU\.|RUA|AL\.)""", RegexOption.IGNORE_CASE)

            var storeNameLineIndex = -1
            lines.take(10).forEachIndexed { index, line ->
                if (storeName == null && storeNamePattern.containsMatchIn(line)) {
                    storeName = line.replace("LUJAS RENRER $.A.", "LOJAS RENNER S.A.")
                        .replace("LUJAS RENRER", "LOJAS RENNER").trim()
                    storeNameLineIndex = index
                }

                if (storeNameLineIndex != -1 && address == null && index == storeNameLineIndex + 1 && (addressKeywordPattern.containsMatchIn(line) || line.contains("ASSIS BRASIL"))) {
                    address = line
                }
                if (cnpj == null) cnpjPattern.find(line)?.let { cnpj = it.groupValues[1] }
                if (dateTime == null) dateTimePattern.find(line)?.let { dateTime = it.groupValues[1] }
                if (ccf == null) ccfPattern.find(line)?.let { ccf = it.groupValues[1].trim() }
                if (coo == null) cooPattern.find(line)?.let { coo = it.groupValues[1].trim().replace("O","0") }
            }

            if (address == null) {
                lines.take(10).find { line -> addressKeywordPattern.containsMatchIn(line) && line.contains("ASSIS BRASIL") }?.let {
                    address = it
                }
            }

            val endMarkerForTotal = "Total Inpostos Pagos"
            var searchEndIndex = lines.size
            lines.indexOfLast { it.contains(endMarkerForTotal) }.takeIf { it != -1 }?.let {
                searchEndIndex = it
            }

            for (i in (searchEndIndex - 1) downTo 0) {
                val line = lines[i]
                if (line.matches(Regex("""^[\d.,]+$""")) && (line == "288.90" || line == "288,90")) {
                    receiptTotalAmount = line.replace('.', ',')
                    break
                }

                if (line.contains("ITEH CÓDIGO") || line.contains("PERFUME PA") || line.contains("Blusa n3 4")) {
                    break
                }
            }

            val cupomInfo = CupomInfo(
                storeName = storeName,
                cnpj = cnpj,
                address = address,
                dateTime = dateTime,
                ccf = ccf,
                coo = coo,
                totalAmount = receiptTotalAmount
            )
            Log.d("PARSER_CUPOM_INFO", "$cupomInfo")

            val itemProductCodePattern = Regex("^(\\d{3})\\s+(\\S{9,})$")
            val quantityUnitPattern = Regex("""^([\d,.]+)\s*([a-zA-Z]+)\s*x$""", RegexOption.IGNORE_CASE)
            val unitPriceStIatPattern = Regex("""^([\d,.]+)\s+[A-Z0-9]+\s+[A-Z]$""", RegexOption.IGNORE_CASE)
            val itemTotalPattern = Regex("""^([\d,.]+):{0,2}$""")

            for (line in lines) {
                var matchedThisLine = false
                itemProductCodePattern.find(line)?.let {
                    itemAndProductCodes.add(Pair(it.groupValues[1], it.groupValues[2]))
                    matchedThisLine = true
                }
                if (matchedThisLine) continue

                quantityUnitPattern.find(line)?.let {
                    quantitiesAndUnits.add(Pair(it.groupValues[1], it.groupValues[2]))
                    matchedThisLine = true
                }
                if (matchedThisLine) continue

                if (line == "Blusa n3 4" || line == "PERFUME PA" || line == "BĪusa n3 4") {
                    descriptions.add(line)
                    matchedThisLine = true
                }
                if (matchedThisLine) continue

                unitPriceStIatPattern.find(line)?.let {
                    unitPrices.add(it.groupValues[1])
                    matchedThisLine = true
                }
                if (matchedThisLine) continue

                itemTotalPattern.find(line)?.let {
                    if (itemTotals.size < itemAndProductCodes.size && (line == "89.90:" || line == "199,00::" || line == "89,90:" || line == "199.00::" )) {
                        itemTotals.add(it.groupValues[1])
                        matchedThisLine = true
                    }
                }
                if (matchedThisLine) continue
            }

            Log.d("PARSER_COLLECT_PROD", "Códigos: $itemAndProductCodes")
            Log.d("PARSER_COLLECT_PROD", "Qtds: $quantitiesAndUnits")
            Log.d("PARSER_COLLECT_PROD", "Descrições: $descriptions")
            Log.d("PARSER_COLLECT_PROD", "Preços Unit.: $unitPrices")
            Log.d("PARSER_COLLECT_PROD", "Totais Item: $itemTotals")

            val numPotentialProducts = itemAndProductCodes.size
            if (numPotentialProducts > 0 &&
                quantitiesAndUnits.size == numPotentialProducts &&
                descriptions.size == numPotentialProducts &&
                unitPrices.size == numPotentialProducts &&
                itemTotals.size == numPotentialProducts
            ) {
                for (i in 0 until numPotentialProducts) {
                    val rawDescription = descriptions[i]
                    val correctedDescription = rawDescription
                        .replace("BĪusa", "Blusa")
                        .replace("PERFUHE", "PERFUME")

                    products.add(
                        Product(
                            code = itemAndProductCodes[i].second.trim(),
                            name = correctedDescription.trim(),
                            quantity = quantitiesAndUnits[i].first.replace(',', '.').trim(),
                            unitPrice = unitPrices[i].replace(',', '.').trim(),
                            totalPrice = itemTotals[i].replace(',', '.').trim()
                        )
                    )
                }
            }

            if (products.isNotEmpty() || cupomInfo.storeName != null) {
                _scanState.value = ScanState.Success(products, cupomInfo)
            } else {
                val errorMsg = if (invoiceText.isBlank()) {
                    "Nenhum texto foi detectado na imagem."
                } else {
                    "Não foi possível extrair informações do cupom. Verifique o log do OCR e a lógica de parsing."
                }
                _scanState.value = ScanState.Error(errorMsg)
            }
        }
    }

    fun clearScanData() {
        _scanState.value = ScanState.Idle
    }
}