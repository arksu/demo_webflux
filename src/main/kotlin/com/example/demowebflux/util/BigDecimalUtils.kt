package com.example.demowebflux.util

import java.math.BigDecimal

val BIGDECIMAL_100 = BigDecimal(100)

fun BigDecimal.percentToMult(scale: Int): BigDecimal {
//    return this.divide(BIGDECIMAL_100, scale, RoundingMode.HALF_UP)
    return this.divide(BIGDECIMAL_100)
}