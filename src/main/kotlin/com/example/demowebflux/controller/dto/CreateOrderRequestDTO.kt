package com.example.demowebflux.controller.dto

import java.math.BigDecimal
import java.util.*

data class CreateOrderRequestDTO(
    val invoiceId: UUID,
    val amount: BigDecimal,
    val selectedCurrency: String,
)