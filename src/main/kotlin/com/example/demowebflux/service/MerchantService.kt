package com.example.demowebflux.service

import com.example.demowebflux.repo.MerchantRepo
import com.example.jooq.tables.pojos.Merchant
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class MerchantService(
    private val merchantRepo: MerchantRepo,
    private val dslContext: DSLContext
) {
    suspend fun getById(id: UUID, ctx: DSLContext? = null): Merchant {
        val merchant = merchantRepo.findById(id, ctx ?: dslContext).awaitSingleOrNull() ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant not found")
        if (!merchant.enabled) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant not found")
        }
        return merchant
    }
}