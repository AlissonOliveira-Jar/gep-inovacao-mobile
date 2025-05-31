package com.unichristus.leitor_fiscal.data

data class CupomInfo(
    val id: Long? = null,
    val storeName: String? = null,
    val cnpj: String? = null,
    val address: String? = null,
    val dateTime: String? = null,
    val ccf: String? = null,
    val coo: String? = null,
    val totalAmount: String? = null,
    val scannedAtTimestamp: Long? = null
)