package com.example.demowebflux.service

import com.example.demowebflux.exception.BadRequestException
import com.example.demowebflux.exception.ShopNotFoundException
import com.example.demowebflux.repo.ShopRepo
import com.example.jooq.tables.pojos.Shop
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.*

/**
 * магазины мерчанта
 */
@Service
class ShopService(
    private val shopRepo: ShopRepo
) {
    suspend fun getById(id: UUID, context: DSLContext): Shop {
        return shopRepo.findById(id, context).awaitSingleOrNull()
            ?: throw ShopNotFoundException(id)
    }

    suspend fun getByApiKey(apiKey: String, context: DSLContext): Shop {
        return shopRepo.findByApiKey(apiKey, context).awaitSingleOrNull()
            ?: throw BadRequestException("Wrong api key")
    }
}