package com.example.demowebflux.service

import com.example.demowebflux.repo.CurrencyRepo
import com.example.jooq.tables.pojos.Currency
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class CurrencyService(
    private val currencyRepo: CurrencyRepo
) {
    val map = HashMap<String, Currency>()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        map.clear()
        currencyRepo.loadAll().doOnNext {
            map[it.name] = it
        }.subscribe()
    }

    fun getByName(name: String): Currency? {
        return map[name]
    }
}