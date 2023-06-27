package com.example.demowebflux.repo

import com.example.jooq.Tables.WEBHOOK_RESULT
import com.example.jooq.tables.pojos.WebhookResult
import com.example.jooq.tables.records.WebhookResultRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Repository
class WebhookResultRepo {
    fun save(entity: WebhookResult, context: DSLContext): Mono<WebhookResult> {
        return context.insertInto(WEBHOOK_RESULT)
            .set(WebhookResultRecord(entity))
            .returning()
            .toMono()
            .map(::WebhookResult)
    }
}