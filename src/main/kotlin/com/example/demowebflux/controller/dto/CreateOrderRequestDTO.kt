package com.example.demowebflux.controller.dto

data class CreateOrderRequestDTO(
    // внешний ид счета
    val invoiceId: String,
    // валюта, которую выбрал клиент
    val selectedCurrency: String,
)