package com.example.demowebflux

import com.example.jooq.Tables.ACCOUNT
import com.example.jooq.tables.pojos.Account
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Repository
class AccountRepo(
    val dslContext: DSLContext
) {
    fun findById(id: UUID): Mono<Account> {

        return dslContext.selectFrom(ACCOUNT)
            .where(ACCOUNT.ID.eq(id))
            .toMono()
            .map(::Account)
    }
}