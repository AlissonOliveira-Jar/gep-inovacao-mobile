package com.unichristus.leitor_fiscal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unichristus.leitor_fiscal.data.Product
import com.unichristus.leitor_fiscal.data.ScanState
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

            val lines = invoiceText.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val products = mutableListOf<Product>()
            var currentIndex = 0

            val productStartPattern = Regex("^\\d+\\s+\\d+\\s+")
            val quantityPriceTotalPattern =
                Regex("(\\d+[,.]\\d+|\\d+)\\s*(KG|UN|PCT|CX)?\\s*x\\s*(\\d+[,.]\\d+)\\s*\\(?.*?\\)?\\s*(\\d+[,.]\\d+)")
            val discountPattern = Regex("Desconto\\s*(-?\\d+[,.]\\d+)")


            while (currentIndex < lines.size) {
                val currentLine = lines[currentIndex]

                if (productStartPattern.find(currentLine) != null) {
                    var productName = ""
                    var quantity = ""
                    var unitPrice = ""
                    var itemTotal = ""
                    var discount = ""

                    val singleLineMatch = quantityPriceTotalPattern.find(currentLine)

                    if (singleLineMatch != null) {
                        quantity = singleLineMatch.groupValues[1]
                        unitPrice = singleLineMatch.groupValues[3]
                        itemTotal = singleLineMatch.groupValues[4]

                        val startMatch = productStartPattern.find(currentLine)
                        val endMatch = quantityPriceTotalPattern.find(currentLine, startIndex = startMatch?.range?.last ?: 0)

                        if (startMatch != null && endMatch != null) {
                            productName = currentLine.substring(startMatch.range.last, endMatch.range.first).trim()
                        } else {
                            val firstPartRemoved = currentLine.replaceFirst(productStartPattern, "").trim()
                            val secondPartRemoved = firstPartRemoved.replace(quantityPriceTotalPattern, "").trim()
                            productName = secondPartRemoved.ifEmpty { "Nome não identificado" }
                        }

                        currentIndex++

                    } else {
                        if (currentIndex + 1 < lines.size) {
                            val nextLine = lines[currentIndex + 1]
                            val multiLineMatch = quantityPriceTotalPattern.find(nextLine)

                            if (multiLineMatch != null) {
                                quantity = multiLineMatch.groupValues[1]
                                unitPrice = multiLineMatch.groupValues[3]
                                itemTotal = multiLineMatch.groupValues[4]

                                val startMatch = productStartPattern.find(currentLine)
                                productName = if (startMatch != null) {
                                    currentLine.substring(startMatch.range.last).trim()
                                } else {
                                    currentLine.trim()
                                }

                                var lookAheadIndex = currentIndex + 2
                                while (lookAheadIndex < lines.size) {
                                    val lookAheadLine = lines[lookAheadIndex]

                                    if (productStartPattern.find(lookAheadLine) != null) {
                                        break
                                    }

                                    val discountMatch = discountPattern.find(lookAheadLine)
                                    if (discountMatch != null) {
                                        discount = discountMatch.groupValues[1]
                                    }

                                    lookAheadIndex++
                                }

                                currentIndex = lookAheadIndex

                            } else {
                                currentIndex++
                                continue
                            }

                        } else {
                            currentIndex++
                            continue
                        }
                    }

                    if (productName.isNotEmpty() && itemTotal.isNotEmpty()) {
                        val cleanQuantity = quantity.replace(',', '.')
                        val cleanUnitPrice = unitPrice.replace(',', '.')
                        val cleanItemTotal = itemTotal.replace(',', '.')
                        val cleanDiscount = discount.replace(',', '.')

                        products.add(
                            Product(
                                code = currentLine.split("\\s+").getOrNull(0) ?: "",
                                name = productName,
                                quantity = cleanQuantity,
                                unitPrice = cleanUnitPrice,
                                totalPrice = cleanItemTotal,
                                discount = cleanDiscount
                            )
                        )
                    }

                } else {
                    currentIndex++
                }
            }

            if (products.isNotEmpty()) {
                _scanState.value = ScanState.Success(products)
            } else {
                _scanState.value = ScanState.Error("Não foi possível extrair os produtos da nota.")
            }
        }
    }

    fun clearScanData() {
        _scanState.value = ScanState.Idle
    }
}