package com.example.demowebflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@EnableWebFlux
@SpringBootApplication
class DemoWebfluxApplication

fun main(args: Array<String>) {
    // disable jooq banner
    System.setProperty("org.jooq.no-logo", "true")
    System.setProperty("org.jooq.no-tips", "true")
    runApplication<DemoWebfluxApplication>(*args)
}
