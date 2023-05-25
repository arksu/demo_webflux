package com.example.demowebflux.util

import java.math.BigDecimal

val BIGDECIMAL_100 = BigDecimal(100)

fun BigDecimal.percentToMult(): BigDecimal {
    return this.divide(BIGDECIMAL_100)
}