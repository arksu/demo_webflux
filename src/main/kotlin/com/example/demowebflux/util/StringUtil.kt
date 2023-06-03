package com.example.demowebflux.util

import kotlin.random.Random

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun randomStringByKotlinRandom(len: Int) = (1..len)
    .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
    .joinToString("")