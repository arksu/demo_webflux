package com.example.demowebflux.controller.dto

import java.math.BigDecimal

data class AvailableCurrenciesResponseDTO(
    val list: List<CurrencyAndRate>
) {
    data class CurrencyAndRate(
        val name: String,
        val displayName: String,

        /**
         * курс обмена на эту валюту от исходной в счете
         */
        val rate: BigDecimal,

        /**
         * сумма по счету в этой валюте
         */
        val amount: BigDecimal
    )
}
