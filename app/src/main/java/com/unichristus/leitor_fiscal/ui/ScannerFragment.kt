package com.unichristus.leitor_fiscal.ui

import android.Manifest
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.unichristus.leitor_fiscal.R
import com.unichristus.leitor_fiscal.data.Product
import com.unichristus.leitor_fiscal.data.ScanState
import com.unichristus.leitor_fiscal.databinding.FragmentScannerBinding
import com.unichristus.leitor_fiscal.ui.adapter.ProductAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()

    private lateinit var productAdapter: ProductAdapter

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(intent)
                scanResult?.let {
                    val imageUri = it.pages?.firstOrNull()?.imageUri
                    if (imageUri != null) {
                        processImageForText(imageUri)
                    } else {
                        Log.e("ScannerFragment", "Nenhum URI de imagem encontrado no resultado.")
                        viewModel.processInvoiceText("")
                    }
                } ?: run {
                    Log.e("ScannerFragment", "Resultado da digitalização nulo.")
                    viewModel.processInvoiceText("")
                }
            } ?: run {
                Log.e("ScannerFragment", "Dados do resultado nulos.")
                viewModel.processInvoiceText("")
            }
        } else {
            Log.e("ScannerFragment", "Digitalização cancelada ou falhou.")
            viewModel.processInvoiceText("")
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
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        when(viewModel.scanState.value) {
            is ScanState.Idle -> clearData()
            is ScanState.Loading -> showLoading(true)
            is ScanState.Success -> {
                val products = (viewModel.scanState.value as ScanState.Success).products
                showProducts(products)
            }
            is ScanState.Error -> {
                val message = (viewModel.scanState.value as ScanState.Error).message
                showError(message)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.recyclerViewProducts.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = productAdapter
        }
        productAdapter.onItemClick = { product ->
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanState.collectLatest { state ->
                    Log.d("ScannerFragment", "Estado do Scanner: $state")
                    when (state) {
                        is ScanState.Idle -> {
                            clearData()
                        }
                        is ScanState.Loading -> {
                            showLoading(true)
                            productAdapter.submitList(emptyList())
                            binding.textViewScannerResult.text = ""
                        }
                        is ScanState.Success -> {
                            showLoading(false)
                            showProducts(state.products)
                        }
                        is ScanState.Error -> {
                            showLoading(false)
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonOpenScanner.setOnClickListener { checkCameraPermission() }

        binding.buttonClearScan.setOnClickListener {
            viewModel.clearScanData()
        }

        binding.textViewScannerResult.setOnLongClickListener {
            val content = binding.textViewScannerResult.text
            if (content.isNotEmpty() &&
                content != getString(R.string.scan_to_start_message) &&
                content != getString(R.string.processing_image_text) &&
                !content.startsWith(getString(R.string.error_prefix))
            ) {
                copyToClipboard(content, getString(R.string.clipboard_scanned_text_label))
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
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("ScannerFragment", "Erro ao iniciar o scanner de documento: ${e.localizedMessage}")
                viewModel.processInvoiceText(getString(R.string.scanner_error_starting) + ": ${e.localizedMessage}")
            }
    }

    private fun processImageForText(imageUri: Uri) {
        viewModel.processInvoiceText(getString(R.string.processing_image_text))

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            val inputImage = InputImage.fromFilePath(requireContext(), imageUri)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    viewModel.processInvoiceText(extractedText)
                }
                .addOnFailureListener { e ->
                    Log.e("ScannerFragment", "Erro ao processar texto da imagem: ${e.localizedMessage}")
                    viewModel.processInvoiceText(getString(R.string.text_extraction_error, e.localizedMessage))
                }
        } catch (e: IOException) {
            Log.e("ScannerFragment", "Erro ao carregar imagem da URI: ${e.localizedMessage}")
            e.printStackTrace()
            viewModel.processInvoiceText(getString(R.string.image_loading_error, e.localizedMessage))
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.recyclerViewProducts.visibility = View.GONE
            binding.textViewScannerResult.visibility = View.GONE
            binding.buttonClearScan.visibility = View.GONE
            binding.textViewScannerLabel.text = getString(R.string.status_label)
        }
    }

    private fun showProducts(products: List<Product>) {
        productAdapter.submitList(products)
        binding.recyclerViewProducts.visibility = View.VISIBLE
        binding.textViewScannerResult.visibility = View.GONE
        binding.buttonClearScan.visibility = View.VISIBLE
        binding.textViewScannerLabel.text = getString(R.string.scanned_text_label)
        if (products.isEmpty()) {
            showError("Nenhum produto identificado.")
        }
    }

    private fun showError(message: String) {
        productAdapter.submitList(emptyList())
        binding.recyclerViewProducts.visibility = View.GONE
        binding.textViewScannerResult.visibility = View.VISIBLE
        binding.textViewScannerResult.text = getString(R.string.error_message_with_prefix, getString(R.string.error_prefix), message)
        binding.buttonClearScan.visibility = View.VISIBLE
        binding.textViewScannerLabel.text = getString(R.string.status_label)
    }

    private fun clearData() {
        productAdapter.submitList(emptyList())
        binding.recyclerViewProducts.visibility = View.GONE
        binding.textViewScannerResult.visibility = View.VISIBLE
        binding.textViewScannerResult.text = getString(R.string.scan_to_start_message)
        binding.buttonClearScan.visibility = View.GONE
        binding.textViewScannerLabel.text = getString(R.string.status_label)
        showLoading(false)
    }

    private fun copyToClipboard(text: CharSequence, label: String) {
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun showPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.camera_permission_needed_title))
            .setMessage(getString(R.string.camera_permission_message))
            .setPositiveButton(getString(R.string.understand_button)) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }.setNegativeButton(getString(R.string.cancel_button), null).show()
        }
}
