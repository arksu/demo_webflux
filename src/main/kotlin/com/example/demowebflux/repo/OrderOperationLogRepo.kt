package com.example.demowebflux.repo

import com.example.jooq.Tables.ORDER_OPERATION_LOG
import com.example.jooq.tables.pojos.OrderOperationLog
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Repository
class OrderOperationLogRepo {
    fun save(entity: OrderOperationLog, context: DSLContext): Mono<OrderOperationLog> {
        return context.insertInto(ORDER_OPERATION_LOG)
            .set(context.newRecord(ORDER_OPERATION_LOG, entity))
            .returning()
            .toMono()
            .map(::OrderOperationLog)
    }
}