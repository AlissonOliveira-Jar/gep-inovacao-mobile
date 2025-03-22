package com.unichristus.leitor_fiscal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.unichristus.leitor_fiscal.databinding.ActivityScannerBinding
import java.util.concurrent.Executors

class ScannerActivity : ComponentActivity() {

    private lateinit var binding: ActivityScannerBinding

    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            processCameraProvider = cameraProviderFuture.get()
            bindCameraPreview()
            bindInputAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder().setTargetRotation(binding.previewView.display.rotation).build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    private fun bindInputAnalyzer() {
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ){
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onScan?.invoke(barcodes)
                    onScan = null
                    finish()
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }

    companion object {
        private var onScan: ((barcodes: List<Barcode>) -> Unit)? = null
        fun startScanner(context: Context, onScan: (barcodes: List<Barcode>) -> Unit) {
            this.onScan = onScan
            Intent(context, ScannerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}