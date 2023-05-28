package com.example.demowebflux.util

import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * в kotlin reactor extensions toFlux объявлена для Publisher и для Iterable
 * компилятор ругается на "Overload resolution ambiguity. All these functions match."
 * объявим тут явно только для Publisher
 */
fun <T : Any> Publisher<T>.toFlux(): Flux<T> = Flux.from(this)
