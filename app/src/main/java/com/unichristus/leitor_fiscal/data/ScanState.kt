package com.unichristus.leitor_fiscal.data

sealed class ScanState {
    data object Loading : ScanState()
    data class Success(val products: List<Product>, val cupomInfo: CupomInfo?) : ScanState()
    data class Error(val message: String) : ScanState()
    data object Idle : ScanState()
}