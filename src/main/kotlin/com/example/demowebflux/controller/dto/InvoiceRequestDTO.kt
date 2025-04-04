package com.example.demowebflux.controller.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class InvoiceRequestDTO(
    @field:NotBlank
    val apiKey: String,

    /**
     * ид клиента в системе мерчанта
     */
    @field:NotBlank
    val customerId: String,

    /**
     * уникальный order id в системе мерчанта
     */
    @field:NotBlank
    val orderId: String,

    /**
     * валюта в которой хочет получить мерчант
     */
    @field:NotBlank
    val currency: String,

    /**
     * сумму которую хочет получить мерчант
     */
    @field:DecimalMin("0.00000001")
    val amount: BigDecimal,

    val description: String?,

    @field:NotBlank
    val successUrl: String,

    @field:NotBlank
    val failUrl: String
)
