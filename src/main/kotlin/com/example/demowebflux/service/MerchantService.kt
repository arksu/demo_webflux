package com.example.demowebflux.service

import com.example.demowebflux.exception.MerchantNotFoundException
import com.example.demowebflux.repo.MerchantRepo
import com.example.jooq.tables.pojos.Merchant
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class MerchantService(
    private val merchantRepo: MerchantRepo,
    private val dslContext: DSLContext
) {
    suspend fun getById(id: UUID, ctx: DSLContext? = null): Merchant {
        val merchant = merchantRepo.findById(id, ctx ?: dslContext).awaitSingleOrNull() ?: throw MerchantNotFoundException(id)
        if (!merchant.enabled) {
            throw MerchantNotFoundException(id)
        }
        return merchant
    }

    /**
     * получить список валют которыми может платить клиент от конкретного мерчанта
     */
    fun getPaymentAvailableCurrencies(merchantId: UUID): List<Int>? {
        return null
    }
}