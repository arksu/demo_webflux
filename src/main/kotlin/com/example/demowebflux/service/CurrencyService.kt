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
    val mapByName = HashMap<String, Currency>()
    val mapById = HashMap<Int, Currency>()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        // TODO !!! блокировка получения значений на время релоада
        mapByName.clear()
        currencyRepo.loadAll().doOnNext {
            mapByName[it.name] = it
            mapById[it.id] = it
        }.subscribe()
    }

    fun getById(id: Int): Currency {
        return mapById[id] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
    }

    fun getByName(name: String): Currency {
        // TODO: воткнуть блокировку во время ожидания reload (асинхронно)
        val currency = mapByName[name]
        if (currency?.enabled == false) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
        return currency ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found")
    }
}