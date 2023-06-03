package com.example.demowebflux.controller.dto

import java.math.BigDecimal

data class InvoiceResponseDTO(
    val id: String,
    val currency: String,
    val amount: BigDecimal,
    val successUrl: String,
    val failUrl: String,
    val paymentUrl: String,
)
