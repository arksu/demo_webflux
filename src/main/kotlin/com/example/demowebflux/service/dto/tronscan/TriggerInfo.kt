package com.example.demowebflux.service.dto.tronscan

data class TriggerInfo(
    val call_value: Int,
    val contract_address: String,
    val method: String,
    val methodId: String,
)