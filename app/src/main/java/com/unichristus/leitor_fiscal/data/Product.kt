package com.unichristus.leitor_fiscal.data

data class Product(
    val code: String,
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val totalPrice: String,
    val discount: String = ""
)

sealed class ScanState {
    object Loading : ScanState()
    data class Success(val products: List<Product>) : ScanState()
    data class Error(val message: String) : ScanState()
    object Idle : ScanState()
}