package com.example.demowebflux.repo

import com.example.jooq.Tables.SHOP
import com.example.jooq.tables.pojos.Shop
import com.example.jooq.tables.records.ShopRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
class ShopRepo : AbstractCrudRepo<UUID, Shop, ShopRecord>() {
    override val table = SHOP
    override val idField = SHOP.ID
    override val mapper = { it: ShopRecord -> Shop(it) }

    fun findByApiKey(apiKey: String, context: DSLContext): Mono<Shop> {
        return context.selectFrom(SHOP)
            .where(SHOP.API_KEY.eq(apiKey))
            .toMonoAndMap()
    }
}