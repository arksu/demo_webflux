package com.example.demowebflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@EnableWebFlux
@SpringBootApplication
class DemoWebfluxApplication

fun main(args: Array<String>) {
    runApplication<DemoWebfluxApplication>(*args)
}
