package com.example.demowebflux.repo

import com.example.jooq.Tables.ACCOUNT
import com.example.jooq.tables.pojos.Account
import org.jooq.DSLContext
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class AccountRepo(
    private val dslContext: DSLContext
) {
    fun findById(id: UUID): Mono<Account> {
        return dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
            .toMono()
//            .map(::Account)
//            .map { it -> Account(it) }
            .map { r -> r.into(Account::class.java) }
    }

    fun findAll(): Flux<Account> {
        return Flux.from(
            dslContext.selectFrom(ACCOUNT)
                .where(ACCOUNT.ID.isNotNull)
        ).map(::Account)
    }
}