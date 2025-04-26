package com.unichristus.leitor_fiscal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.unichristus.leitor_fiscal.R
import com.unichristus.leitor_fiscal.databinding.FragmentScannerBinding
import java.io.IOException

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(intent)
                scanResult?.let {
                    val imageUris = it.pages?.map { page -> page.imageUri }
                    if (imageUris?.isNotEmpty() == true) {
                        processImageForText(imageUris[0])
                    } else {
                        binding.textViewScannerResult.text = getString(R.string.no_image_for_text)
                        Log.e("ScannerFragment", "Nenhum URI de imagem encontrado no resultado.")
                    }
                } ?: run {
                    binding.textViewScannerResult.text = getString(R.string.scanner_result_null)
                    Log.e("ScannerFragment", "Dados do resultado nulos.")
                }
            } ?: run {
                binding.textViewScannerResult.text = getString(R.string.scanner_result_null)
                Log.e("ScannerFragment", "Dados do resultado nulos.")
            }
        } else {
            binding.textViewScannerResult.text = getString(R.string.scanner_cancelled_failed)
            Log.e("ScannerFragment", "Digitalização cancelada ou falhou.")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanner()
        } else {
            showPermissionRationale()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val lastScannedText = sharedPref.getString("last_scanned_text", "")

        if (lastScannedText?.isNotEmpty() == true) {
            binding.textViewScannerResult.text = lastScannedText
            binding.textViewScannerLabel.text = getString(R.string.last_scanned_text_label)
        } else {
            binding.textViewScannerResult.text = getString(R.string.scan_to_start_message)
            binding.textViewScannerLabel.text = getString(R.string.status_label)
        }
        binding.buttonOpenScanner.text = getString(R.string.scan_document_button)
        binding.buttonOpenScanner.setOnClickListener { checkCameraPermission() }

        binding.textViewScannerResult.setOnLongClickListener {
            val content = binding.textViewScannerResult.text
            if (content.isNotEmpty() &&
                content != getString(R.string.scan_to_start_message) &&
                !content.startsWith(getString(R.string.status_label))
            ) {
                copyToClipboard(content)
                Toast.makeText(context, getString(R.string.text_copied_toast), Toast.LENGTH_SHORT).show()
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
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("ScannerFragment", "Erro ao iniciar o scanner de documento: ${e.localizedMessage}")
                binding.textViewScannerResult.text = getString(R.string.scanner_error_starting)
            }
    }

    private fun processImageForText(imageUri: Uri) {

        binding.textViewScannerResult.text = getString(R.string.processing_image_text)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            val inputImage = InputImage.fromFilePath(requireContext(), imageUri)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    updateUIWithTextResult(extractedText)
                }
                .addOnFailureListener { e ->
                    Log.e("ScannerFragment", "Erro ao processar texto da imagem: ${e.localizedMessage}")
                    updateUIWithTextResult(getString(R.string.text_extraction_error, e.localizedMessage))
                }
        } catch (e: IOException) {
            Log.e("ScannerFragment", "Erro ao carregar imagem da URI: ${e.localizedMessage}")
            updateUIWithTextResult(getString(R.string.image_loading_error, e.localizedMessage))
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIWithTextResult(extractedText: String) {
        if (extractedText.isNotEmpty()) {
            binding.textViewScannerResult.text = extractedText

            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            with (sharedPref.edit()) {
                putString("last_scanned_text", extractedText)
                apply()
            }

        } else {
            binding.textViewScannerResult.text = getString(R.string.no_text_found)
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            with (sharedPref.edit()) {
                remove("last_scanned_text")
                apply()
            }
        }
        binding.textViewScannerLabel.text = getString(R.string.scanned_text_label)
    }

    private fun copyToClipboard(text: CharSequence) {
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(getString(R.string.clipboard_scanned_text_label), text)
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
}
