package com.example.demowebflux.service.dto.tronscan

import java.math.BigDecimal

data class ContractData(
    val amount: BigDecimal?,
    val owner_address: String,
    val contract_address: String?,
    val to_address: String?,
)