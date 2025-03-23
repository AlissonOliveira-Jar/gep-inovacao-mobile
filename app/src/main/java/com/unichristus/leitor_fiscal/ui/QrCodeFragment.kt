package com.unichristus.leitor_fiscal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import com.unichristus.leitor_fiscal.ScannerActivity
import com.unichristus.leitor_fiscal.databinding.FragmentQrCodeBinding

class QrCodeFragment : Fragment() {
    private var _binding: FragmentQrCodeBinding? = null
    private val binding get() = _binding!!

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val barcodeValues = result.data?.getStringArrayListExtra("barcodes")
            val barcodeTypes = result.data?.getIntegerArrayListExtra("barcodeTypes")
            if (barcodeValues != null && barcodeTypes != null) {
                for (i in barcodeValues.indices) {
                    updateUI(barcodeValues[i], barcodeTypes[i])
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startScanner()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonOpenScanner.setOnClickListener { checkCameraPermission() }

        binding.textViewQrContent.setOnLongClickListener {
            val content = binding.textViewQrContent.text
            if (content.isNotEmpty()) {
                copyToClipboard(content)
                Toast.makeText(context, "Conteúdo copiado!", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> startScanner()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionRationale()

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanner() {
        ScannerActivity.startScanner(
            requireContext(),
            listOf(Barcode.FORMAT_QR_CODE)
        )
        scannerLauncher.launch(Intent(context, ScannerActivity::class.java))
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(rawValue: String, valueType: Int) {
        if (rawValue.length >= 44 && rawValue.contains("|")) {
            binding.textViewQrType.text = "Tipo: Nota Fiscal Eletrônica (NF-e)"
            binding.textViewQrContent.text = "Chave de Acesso: ${rawValue.substringBefore("|")}"
            processNfeContent(rawValue)
        } else {
            val formattedType = when (valueType) {
                Barcode.TYPE_URL -> "URL"
                Barcode.TYPE_CONTACT_INFO -> "Contato"
                Barcode.TYPE_CALENDAR_EVENT -> "Evento"
                Barcode.TYPE_EMAIL -> "E-mail"
                Barcode.TYPE_PHONE -> "Telefone"
                Barcode.TYPE_SMS -> "SMS"
                Barcode.TYPE_WIFI -> "Wi-Fi"
                Barcode.TYPE_GEO -> "Localização"
                Barcode.TYPE_DRIVER_LICENSE -> "CNH"
                else -> "Outro Tipo"
            }
            binding.textViewQrType.text = "Tipo: $formattedType"
            binding.textViewQrContent.text = formatBarcodeData(rawValue, valueType)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun processNfeContent(nfeRawData: String) {
        // Descobrir como faz isso
    }

    private fun formatBarcodeData(rawValue: String, valueType: Int): String {
        return when (valueType) {
            Barcode.TYPE_URL -> "URL: $rawValue"
            Barcode.TYPE_CONTACT_INFO -> "Contato: $rawValue"
            Barcode.TYPE_WIFI -> "Wi-Fi: $rawValue"
            Barcode.TYPE_CALENDAR_EVENT -> "Evento: $rawValue"
            Barcode.TYPE_EMAIL -> "E-mail: $rawValue"
            else -> "Conteúdo: $rawValue"
        }
    }

    private fun copyToClipboard(text: CharSequence) {
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Conteúdo do QR Code", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun showPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permissão de Câmera Necessária")
            .setMessage("O aplicativo precisa da permissão da câmera para escanear códigos QR.")
            .setPositiveButton("Entendi") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}