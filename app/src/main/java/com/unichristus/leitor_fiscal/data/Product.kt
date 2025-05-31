package com.unichristus.leitor_fiscal.data

import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val totalPrice: String,
    val discount: String = "0.0"
)