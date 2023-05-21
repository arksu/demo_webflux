package com.example.demowebflux.service

import com.example.demowebflux.repo.CurrencyRepo
import com.example.jooq.tables.pojos.Currency
import jakarta.annotation.PostConstruct
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

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

    fun getByName(name: String): Currency {
        // TODO: воткнуть блокировку во время ожидания reload (асинхронно)
        val currency = map[name]
        if (currency?.enabled == false) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
        return currency ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
    }
}