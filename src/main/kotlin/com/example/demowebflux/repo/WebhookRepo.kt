package com.example.demowebflux.repo

import com.example.jooq.Tables.WEBHOOK
import com.example.jooq.tables.pojos.Webhook
import com.example.jooq.tables.records.WebhookRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class WebhookRepo : AbstractCrudRepo<Long, Webhook, WebhookRecord>() {
    override val table = WEBHOOK
    override val idField = WEBHOOK.ID
    override val mapper = { it: WebhookRecord -> Webhook(it) }

    fun findAllIsNotCompletedLimit(limit: Int, context: DSLContext): Flux<Webhook> {
        return context.selectFrom(WEBHOOK)
            .where(WEBHOOK.IS_COMPLETED.isFalse)
            .limit(limit)
            .toFluxAndMap()
    }

    fun incTryCount(webhook: Webhook, context: DSLContext): Mono<Webhook> {
        return context.update(WEBHOOK)
            .set(WEBHOOK.TRY_COUNT, WEBHOOK.TRY_COUNT.plus(1))
            .where(WEBHOOK.ID.eq(webhook.id))
            .returning()
            .toMonoAndMap()
    }

    fun setCompleted(id: Long, context: DSLContext): Mono<Webhook> {
        return context.update(WEBHOOK)
            .set(WEBHOOK.IS_COMPLETED, true)
            .returning()
            .toMonoAndMap()
    }

    fun incErrorCount(webhook: Webhook, context: DSLContext): Mono<Webhook> {
        return context.update(WEBHOOK)
            .set(WEBHOOK.ERROR_COUNT, WEBHOOK.ERROR_COUNT.plus(1))
            .where(WEBHOOK.ID.eq(webhook.id))
            .returning()
            .toMonoAndMap()
    }
}