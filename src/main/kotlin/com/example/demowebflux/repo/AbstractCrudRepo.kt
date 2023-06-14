package com.example.demowebflux.repo

import org.jooq.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

abstract class AbstractCrudRepo<ID, T, R : UpdatableRecord<R>> {
    abstract val table: Table<R>
    abstract val idField: Field<ID>
    abstract val mapper: (R) -> T

    fun findById(id: ID, context: DSLContext): Mono<T> {
        return context.selectFrom(table)
            .where(idField.eq(id))
            .toMonoAndMap()
    }

    fun findByIdForUpdateSkipLocked(id: ID, context: DSLContext): Mono<T> {
        return context.selectFrom(table)
            .where(idField.eq(id))
            .forUpdate()
            .skipLocked()
            .toMonoAndMap()
    }

    fun save(entity: T, context: DSLContext): Mono<T> {
        return context.insertInto(table)
            .set(context.newRecord(table, entity))
            .returning()
            .toMonoAndMap()
    }

    fun ResultQuery<R>.toMonoAndMap(): Mono<T> {
        return Mono.from(this).map(mapper)
    }

    fun ResultQuery<R>.toFluxAndMap(): Flux<T> {
        return Flux.from(this).map(mapper)
    }
}