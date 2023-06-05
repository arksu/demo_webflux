package com.example.demowebflux.controller.dto

import java.math.BigDecimal

/**
 * DTO - для мерчанта
 */
data class MerchantInvoiceResponseDTO(
    val id: String,
    val currency: String,
    val amount: BigDecimal,
    val paymentUrl: String,
)
