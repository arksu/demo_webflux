package com.example.demowebflux.controller.dto

data class CreateOrderRequestDTO(
    val invoiceId: String,
    val selectedCurrency: String,
)