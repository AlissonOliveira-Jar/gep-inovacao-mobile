package com.unichristus.leitor_fiscal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.unichristus.leitor_fiscal.databinding.FragmentScannerBinding

class ScannerFragment : Fragment() {
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var scannedImageView: ImageView // Para exibir a imagem digitalizada

    // Usar StartIntentSenderForResult em vez de StartActivityForResult
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(intent)
                scanResult?.let {
                    val pdfUri = it.pdf?.uri // URI do PDF, se gerado
                    val imageUris = it.pages?.map { page -> page.imageUri } // Lista de URIs das imagens
                    if (imageUris?.isNotEmpty() == true) {
                        updateUI(imageUris[0]) // Usa a primeira imagem como exemplo
                    } else if (pdfUri != null) {
                        updateUI(pdfUri) // Usa o PDF se não houver imagens
                    } else {
                        Log.e("ScannerFragment", "Nenhum URI encontrado no resultado.")
                    }
                }
            } ?: Log.e("ScannerFragment", "Dados do resultado nulos.")
        } else {
            Log.e("ScannerFragment", "Digitalização cancelada ou falhou.")
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
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scannedImageView = binding.imageViewScanned // Associa o ImageView
        binding.buttonOpenScanner.setOnClickListener { checkCameraPermission() }
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
            .setGalleryImportAllowed(true) // Permite importar da galeria
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // Modo de digitalização completo
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF, GmsDocumentScannerOptions.RESULT_FORMAT_JPEG) // Gera PDF e JPEG
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        scanner.getStartScanIntent(requireActivity()) // Passar Activity em vez de Context
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("ScannerFragment", "Erro ao iniciar o scanner: ${e.localizedMessage}")
            }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(uri: Uri) {
        scannedImageView.setImageURI(uri)
        binding.textViewScannerResult.text = "Imagem digitalizada salva em: $uri"
    }

    private fun showPermissionRationale() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}