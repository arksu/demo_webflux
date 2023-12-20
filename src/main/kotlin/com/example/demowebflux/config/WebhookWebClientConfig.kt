package com.example.demowebflux.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class WebhookWebClientConfig(
    @Value("\${app.webhookUserAgent}")
    private val webhookUserAgent: String
) {
    @Bean
    fun webhookClient(): WebClient {
        return WebClient.builder()
            // ограничиваем размер принимаемого ответа
            .filter(ExchangeFilterFunctions.limitResponseSize(4096))
            .defaultHeaders {
                it.set(HttpHeaders.USER_AGENT, webhookUserAgent)
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient
                        .create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .responseTimeout(Duration.ofSeconds(10))
                )
            )
            .build()
    }
}