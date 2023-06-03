package com.example.demowebflux.repo

import org.jooq.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

abstract class AbstractCrudRepo<ID, T, R : UpdatableRecord<R>> {
    abstract val table: Table<R>
    abstract val idField: Field<ID>
    abstract val mapper: (R) -> T

    fun findById(id: ID, context: DSLContext): Mono<T> {
        return context.selectFrom(table)
            .where(idField.eq(id))
            .toMonoAndMap()
    }

    fun save(entity: T, context: DSLContext): Mono<T> {
        return context.insertInto(table)
            .set(context.newRecord(table, entity))
            .returning()
            .toMonoAndMap()
    }

    fun ResultQuery<R>.toMonoAndMap() : Mono<T> {
        return Mono.from(this).map(mapper)
    }
}