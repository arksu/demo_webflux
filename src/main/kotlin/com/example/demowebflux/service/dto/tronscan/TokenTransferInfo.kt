package com.example.demowebflux.service.dto.tronscan

data class TokenTransferInfo(
    val amount_str: String,
    val contract_address: String,
    val decimals: Int,
    val from_address: String,
    val icon_url: String?,
    val level: String?,
    val name: String,
    val status: Int,
    val symbol: String,
    val to_address: String,
    val tokenType: String,
    val type: String,
)