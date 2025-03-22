package com.unichristus.leitor_fiscal

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import com.unichristus.leitor_fiscal.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScanner()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonOpenScanner.setOnClickListener {
            requestCameraAndStartScanner()
        }
    }

    private fun requestCameraAndStartScanner() {
        if (isPermissionGranted(cameraPermission)) {
            startScanner()
        } else {
            requestCameraPermission()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcodes ->
            barcodes.forEach { barcode ->
                val formattedType = when (barcode.valueType) {
                    Barcode.TYPE_URL -> "URL"
                    Barcode.TYPE_CONTACT_INFO -> "Contato"
                    Barcode.TYPE_CALENDAR_EVENT -> "Evento"
                    Barcode.TYPE_EMAIL -> "E-mail"
                    Barcode.TYPE_PHONE -> "Telefone"
                    Barcode.TYPE_SMS -> "SMS"
                    Barcode.TYPE_WIFI -> "Wi-Fi"
                    Barcode.TYPE_GEO -> "Localization"
                    Barcode.TYPE_DRIVER_LICENSE -> "CNH"
                    else -> "Outro Tipo"
                }
                binding.textViewQrType.text = "Tipo: $formattedType"

                binding.textViewQrContent.text = formatBarcodeData(barcode)
            }
        }
    }

    private fun formatBarcodeData(barcode: Barcode): String {
        return when (barcode.valueType) {
            Barcode.TYPE_URL -> "URL: ${barcode.url?.url}"
            Barcode.TYPE_CONTACT_INFO -> """
            Nome: ${barcode.contactInfo?.name?.formattedName}
            Telefone: ${barcode.contactInfo?.phones?.firstOrNull()?.number}
            E-mail: ${barcode.contactInfo?.emails?.firstOrNull()?.address}
            Empresa: ${barcode.contactInfo?.organization}
        """.trimIndent()

            Barcode.TYPE_WIFI -> """
            SSID: ${barcode.wifi?.ssid}
            Senha: ${barcode.wifi?.password}
            Tipo: ${barcode.wifi?.encryptionType}
        """.trimIndent()

            Barcode.TYPE_CALENDAR_EVENT -> """
            Evento: ${barcode.calendarEvent?.summary}
            Local: ${barcode.calendarEvent?.location}
            Início: ${barcode.calendarEvent?.start}
            Fim: ${barcode.calendarEvent?.end}
        """.trimIndent()

            Barcode.TYPE_EMAIL -> """
            Assunto: ${barcode.email?.subject}
            Corpo: ${barcode.email?.body}
            Endereço: ${barcode.email?.address}
        """.trimIndent()

            else -> "Conteúdo: ${barcode.rawValue ?: "Não decodificado"}"
        }
    }

    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest {
                    openPermissionSetting()
                }
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
}
