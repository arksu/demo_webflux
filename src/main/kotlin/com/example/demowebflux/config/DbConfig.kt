package com.example.demowebflux.config

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class DbConfig {

    @Bean
    fun dslContext(cf: ConnectionFactory): DSLContext {
        return DSL.using(cf)
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.create()
    }
}