package com.example.demowebflux.config

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DbConfig {
    @Bean
    fun connectionFactory(): ConnectionFactory {
        val options = ConnectionFactoryOptions.parse("r2dbc:postgresql://localhost:5432/demo_webflux")
            .mutate()
            .option(ConnectionFactoryOptions.USER, "demo_webflux")
            .option(ConnectionFactoryOptions.PASSWORD, "demo_webflux")
            .build()
        return ConnectionFactories.get(options)
    }

    @Bean
    fun dslContext(cf: ConnectionFactory): DSLContext {
        return DSL.using(cf)
    }
}