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
import com.unichristus.leitor_fiscal.R
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
            if (barcodeValues != null && barcodeTypes != null && barcodeValues.isNotEmpty()) {
                updateUI(barcodeValues[0], barcodeTypes[0])
                saveQrCodeResult(barcodeValues[0], barcodeTypes[0])
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startScanner()
        else showPermissionRationale()
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

        loadQrCodeResult()

        binding.buttonOpenScanner.setOnClickListener { checkCameraPermission() }

        binding.textViewQrContent.setOnLongClickListener {
            val content = binding.textViewQrContent.text
            if (content.isNotEmpty()) {
                copyToClipboard(content, getString(R.string.clipboard_qr_code_label))
                Toast.makeText(context, getString(R.string.content_copied_toast), Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }

        binding.buttonClearQr.text = getString(R.string.button_clear_scan)
        binding.buttonClearQr.setOnClickListener {
            binding.textViewQrType.text = ""
            binding.textViewQrContent.text = ""

            clearQrCodeResult()

            Toast.makeText(context, getString(R.string.qr_cleared_toast), Toast.LENGTH_SHORT).show()
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
        val scannerIntent = Intent(requireContext(), ScannerActivity::class.java).apply {
            putIntegerArrayListExtra("formats", ArrayList(listOf(Barcode.FORMAT_QR_CODE)))
        }
        scannerLauncher.launch(scannerIntent)
    }

    @SuppressLint("SetTextI18n")
    fun updateUI(rawValue: String, valueType: Int) {
        if (rawValue.length >= 44 && rawValue.contains("|")) {
            binding.textViewQrType.text = getString(R.string.nfe_type_label)
            binding.textViewQrContent.text = "${getString(R.string.nfe_access_key_label)} ${rawValue.substringBefore("|")}"
            processNfeContent(rawValue)
        } else {
            val formattedType = when (valueType) {
                Barcode.TYPE_URL -> getString(R.string.url_label)
                Barcode.TYPE_CONTACT_INFO -> getString(R.string.contact_label)
                Barcode.TYPE_CALENDAR_EVENT -> getString(R.string.event_label)
                Barcode.TYPE_EMAIL -> getString(R.string.email_label)
                Barcode.TYPE_PHONE -> getString(R.string.phone_label)
                Barcode.TYPE_SMS -> getString(R.string.sms_label)
                Barcode.TYPE_WIFI -> getString(R.string.wifi_label)
                Barcode.TYPE_GEO -> getString(R.string.geo_label)
                Barcode.TYPE_DRIVER_LICENSE -> getString(R.string.driver_license_label)
                else -> getString(R.string.other_type_label)
            }
            binding.textViewQrType.text = formattedType
            binding.textViewQrContent.text = formatBarcodeData(rawValue, valueType)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun processNfeContent(nfeRawData: String) {

    }

    private fun formatBarcodeData(rawValue: String, valueType: Int): String {
        return when (valueType) {
            Barcode.TYPE_URL -> rawValue
            Barcode.TYPE_CONTACT_INFO -> rawValue
            Barcode.TYPE_WIFI -> rawValue
            Barcode.TYPE_CALENDAR_EVENT -> rawValue
            Barcode.TYPE_EMAIL -> rawValue
            else -> rawValue
        }
    }

    private fun copyToClipboard(text: CharSequence, label: String) {
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun showPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.camera_permission_needed_title))
            .setMessage(getString(R.string.qr_barcode_permission_message))
            .setPositiveButton(getString(R.string.understand_button)) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveQrCodeResult(rawValue: String, valueType: Int) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString("last_qr_value", rawValue)
            putInt("last_qr_type", valueType)
            commit()
        }
    }

    private fun loadQrCodeResult() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val savedValue = sharedPref.getString("last_qr_value", null)
        val savedType = sharedPref.getInt("last_qr_type", -1)

        if (savedValue != null && savedType != -1) {
            updateUI(savedValue, savedType)
        }
    }

    private fun clearQrCodeResult() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            remove("last_qr_value")
            remove("last_qr_type")
            apply()
        }
    }
}