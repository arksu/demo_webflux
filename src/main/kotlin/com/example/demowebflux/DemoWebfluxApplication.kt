package com.example.demowebflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cglib.core.Local
import org.springframework.web.reactive.config.EnableWebFlux
import java.util.Locale

@EnableWebFlux
@SpringBootApplication
class DemoWebfluxApplication

fun main(args: Array<String>) {
    // no translate for error's details
    Locale.setDefault(Locale.ROOT)

    // disable jooq banner
    System.setProperty("org.jooq.no-logo", "true")
    System.setProperty("org.jooq.no-tips", "true")

    runApplication<DemoWebfluxApplication>(*args)
}
