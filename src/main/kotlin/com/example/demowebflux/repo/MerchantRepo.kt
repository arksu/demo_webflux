package com.example.demowebflux.repo

import com.example.jooq.Tables.MERCHANT
import com.example.jooq.tables.pojos.Merchant
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class MerchantRepo(private val dslContext: DSLContext) {
    fun findById(id: UUID): Mono<Merchant> {
        return dslContext.selectFrom(MERCHANT)
            .where(MERCHANT.ID.eq(id))
            .toMono()
            .map(::Merchant)
    }
}