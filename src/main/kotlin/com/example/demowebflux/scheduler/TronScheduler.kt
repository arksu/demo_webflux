package com.example.demowebflux.scheduler

import com.example.demowebflux.repo.BlockchainIncomeWalletRepo
import com.example.demowebflux.service.CurrencyService
import com.example.demowebflux.service.ExchangeRateService
import com.example.jooq.enums.BlockchainType
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@DependsOn("currencyService")
class TronScheduler(
    private val blockchainIncomeWalletRepo: BlockchainIncomeWalletRepo,
    private val currencyService: CurrencyService,
    private val dslContext: DSLContext,
    private val exchangeRateService: ExchangeRateService,
) {
    val nileCurrencies: MutableList<Int> = ArrayList()
    val shastaCurrencies: MutableList<Int> = ArrayList()
    val prodCurrencies: MutableList<Int> = ArrayList()

    @PostConstruct
    fun init() {
        prodCurrencies.clear()
        prodCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON).map {
            it.id
        })
        shastaCurrencies.clear()
        shastaCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_SHASTA).map {
            it.id
        })
        nileCurrencies.clear()
        nileCurrencies.addAll(currencyService.getByBlockchain(BlockchainType.TRON_NILE).map {
            it.id
        })
    }

    @Scheduled(fixedDelay = 5000)
    fun update() {
        runBlocking {
            exchangeRateService.getRatesFromBinance()
        }

        // найдем занятые кошельки по трону
        blockchainIncomeWalletRepo
            .findIsBusy(nileCurrencies, dslContext)
            .doOnNext {
                // TODO
//                println(it)
                val some = it
            }.subscribe()
    }
}