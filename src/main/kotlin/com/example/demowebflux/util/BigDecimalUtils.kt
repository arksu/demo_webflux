package com.example.demowebflux.util

import java.math.BigDecimal

val BIGDECIMAL_100 = BigDecimal(100)

fun BigDecimal.percentToMult(scale: Int): BigDecimal {
    if (this.signum() == 0) return BigDecimal.ZERO

//    return this.divide(BIGDECIMAL_100, scale, RoundingMode.HALF_UP)
    return this.divide(BIGDECIMAL_100)
}