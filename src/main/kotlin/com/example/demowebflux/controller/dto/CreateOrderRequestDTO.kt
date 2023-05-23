package com.example.demowebflux.controller.dto

import java.util.*

data class CreateOrderRequestDTO(
    val invoiceId: UUID,
    val selectedCurrency: String,
)