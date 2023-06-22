package com.example.demowebflux.service

import com.example.jooq.tables.pojos.Shop
import com.example.jooq.tables.pojos.Webhook
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val mapper: ObjectMapper
) {
    fun send(shop: Shop, body: Any) {
        val webhook = Webhook()
        webhook.shopId = shop.id
        webhook.url = shop.webhookUrl
        webhook.requestBody = mapper.writeValueAsString(body)
        webhook.isCompleted = false
        webhook.tries = 0
    }
}