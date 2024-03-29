package com.example.demowebflux.util

import java.security.SecureRandom
import kotlin.random.Random

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
val charHexPool: List<Char> = ('a'..'f') + ('0'..'9')

fun randomStringByKotlinRandom(len: Int) = (1..len)
    .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
    .joinToString("")

fun randomHexStringByKotlinRandom(len: Int): String {
    val r = SecureRandom()
    return (1..len)
        .map { r.nextInt(0, charHexPool.size).let { charHexPool[it] } }
        .joinToString("")
}