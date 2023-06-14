package com.example.demowebflux.service.dto.tronscan

data class Cost(
    val energy_fee: Int,
    val energy_fee_cost: Int,
    val energy_penalty_total: Int,
    val energy_usage: Int,
    val energy_usage_total: Int,
    val fee: Int,
    val memoFee: Int,
    val multi_sign_fee: Int,
    val net_fee: Int,
    val net_fee_cost: Int,
    val net_usage: Int,
    val origin_energy_usage: Int
)