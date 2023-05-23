package com.example.demowebflux.controller.dto

import com.example.jooq.enums.CommissionType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.*

data class InvoiceRequestDTO(
    val merchantId: UUID,
    @field:NotBlank
    val customerId: String,
    @field:NotBlank
    val orderId: String,
    @field:NotBlank
    val currency: String,
    @field:DecimalMin("0.00000001")
    val amount: BigDecimal,
    val description: String?,
    @field:NotBlank
    val successUrl: String,
    @field:NotBlank
    val failUrl: String,
    val commissionCharge: CommissionType = CommissionType.CLIENT
)
