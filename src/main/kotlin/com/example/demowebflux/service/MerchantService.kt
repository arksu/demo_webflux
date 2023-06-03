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
) {
    suspend fun getById(id: UUID, context: DSLContext): Merchant {
        val merchant = merchantRepo.findById(id, context).awaitSingleOrNull() ?: throw MerchantNotFoundException(id)
        if (!merchant.enabled) {
            throw MerchantNotFoundException(id)
        }
        return merchant
    }

    /**
     * получить список валют которыми может платить клиент от конкретного мерчанта
     */
    fun getPaymentAvailableCurrencies(merchant: Merchant): List<Int>? {
        return null
    }
}