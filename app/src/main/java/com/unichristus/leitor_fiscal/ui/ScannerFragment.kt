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
import com.unichristus.leitor_fiscal.data.CupomInfo
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
                        Log.e("ScannerFragment", "Nenhum URI de imagem encontrado no resultado do GMS Scanner.")
                        viewModel.processInvoiceText("")
                    }
                } ?: run {
                    Log.e("ScannerFragment", "Resultado da digitalização GMS nulo.")
                    viewModel.processInvoiceText("")
                }
            } ?: run {
                Log.e("ScannerFragment", "Dados do resultado GMS nulos.")
                viewModel.processInvoiceText("")
            }
        } else {
            Log.e("ScannerFragment", "Digitalização GMS cancelada ou falhou.")
            viewModel.processInvoiceText("")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startGmsDocumentScanner()
        } else {
            showPermissionRationale()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("ScannerFragment", "Imagem original selecionada da galeria: $it")
            processImageForText(it)
        } ?: run {
            Log.e("ScannerFragment", "Nenhuma imagem selecionada.")
            viewModel.processInvoiceText("")
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
        setupClickListeners()
        setupObservers()
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
                    Log.d("ScannerFragment", "Novo Estado do Scanner (Fragment): $state")
                    when (state) {
                        is ScanState.Idle -> {
                            clearData()
                        }
                        is ScanState.Loading -> {
                            showLoading(true)
                        }
                        is ScanState.Success -> {
                            showLoading(false)
                            displayCupomInfo(state.cupomInfo)
                            showProducts(state.products)
                            if (state.products.isNotEmpty() || state.cupomInfo?.storeName != null) {
                                binding.textViewScannerResult.visibility = View.GONE
                            } else {
                                binding.textViewScannerResult.text = getString(R.string.no_data_found)
                                binding.textViewScannerResult.visibility = View.VISIBLE
                            }
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
        binding.buttonOpenScanner.setOnClickListener {
            pickImageDirectly()
        }

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
            ) == PackageManager.PERMISSION_GRANTED -> startGmsDocumentScanner()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionRationale()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startGmsDocumentScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
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

    private fun pickImageDirectly() {
        imagePickerLauncher.launch("image/*")
    }

    private fun processImageForText(imageUri: Uri) {
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
        if (_binding == null) return

        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.recyclerViewProducts.visibility = View.GONE
            binding.textViewScannerResult.text = getString(R.string.processing_image_text)
            binding.textViewScannerResult.visibility = View.VISIBLE
            binding.buttonClearScan.visibility = View.GONE
            binding.textViewScannerLabel.text = getString(R.string.status_label)
            clearCupomInfoDisplay()
        }
    }

    private fun displayCupomInfo(cupomInfo: CupomInfo?) {
        if (_binding == null) return

        val hasCupomData = cupomInfo?.storeName != null || cupomInfo?.cnpj != null || cupomInfo?.address != null ||
                cupomInfo?.dateTime != null || cupomInfo?.ccf != null || cupomInfo?.coo != null ||
                cupomInfo?.totalAmount != null

        binding.layoutCupomInfo.visibility = if(hasCupomData) View.VISIBLE else View.GONE

        binding.textViewStoreName.text = getString(R.string.store_name_label, cupomInfo?.storeName ?: "")
        binding.textViewCnpj.text = getString(R.string.cnpj_label, cupomInfo?.cnpj ?: "")
        binding.textViewDateTime.text = getString(R.string.date_time_label, cupomInfo?.dateTime ?: "")
        binding.textViewAddress.text = getString(R.string.address_label, cupomInfo?.address ?: "")
        binding.textViewCcf.text = getString(R.string.ccf_coo_label, cupomInfo?.ccf ?: "-", cupomInfo?.coo ?: "-")
        binding.textViewReceiptTotal.text = getString(R.string.receipt_total_label, cupomInfo?.totalAmount ?: "0,00")
    }

    private fun clearCupomInfoDisplay() {
        if (_binding == null) return

        binding.layoutCupomInfo.visibility = View.GONE
        binding.textViewStoreName.text = ""
        binding.textViewCnpj.text = ""
        binding.textViewDateTime.text = ""
        binding.textViewAddress.text = ""
        binding.textViewCcf.text = ""
        binding.textViewReceiptTotal.text = ""
    }

    private fun showProducts(products: List<Product>) {
        if (_binding == null) return
        productAdapter.submitList(products)
        binding.recyclerViewProducts.visibility = if (products.isNotEmpty()) View.VISIBLE else View.GONE
        binding.buttonClearScan.visibility = View.VISIBLE
        binding.textViewScannerLabel.text = if (products.isNotEmpty()) getString(R.string.scanned_items_label) else getString(R.string.status_label)

        if (products.isEmpty() && viewModel.scanState.value is ScanState.Success) {
            val successState = viewModel.scanState.value as ScanState.Success
            if (successState.cupomInfo?.storeName == null) {
                binding.textViewScannerLabel.text = getString(R.string.no_data_found)
            } else if (successState.products.isEmpty()) {
                binding.textViewScannerLabel.text = getString(R.string.no_products_identified)
            }
        }
    }

    private fun showError(message: String) {
        if (_binding == null) return
        productAdapter.submitList(emptyList())
        binding.recyclerViewProducts.visibility = View.GONE
        clearCupomInfoDisplay()
        binding.textViewScannerResult.visibility = View.VISIBLE
        binding.textViewScannerResult.text = getString(R.string.error_message_with_prefix, getString(R.string.error_prefix), message)
        binding.buttonClearScan.visibility = View.VISIBLE
        binding.textViewScannerLabel.text = getString(R.string.status_label)
    }

    private fun clearData() {
        if (_binding == null) return
        productAdapter.submitList(emptyList())
        binding.recyclerViewProducts.visibility = View.GONE
        clearCupomInfoDisplay()
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