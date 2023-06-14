package com.example.demowebflux.service.dto.tronscan

data class ContractInfo(
    val isToken: Boolean,
    val name: String,
    val risk: Boolean,
    val vip: Boolean
)