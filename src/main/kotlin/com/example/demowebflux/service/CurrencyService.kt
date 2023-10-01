package com.example.demowebflux.service

import com.example.demowebflux.exception.CurrencyNotFoundException
import com.example.demowebflux.repo.CurrencyRepo
import com.example.jooq.enums.BlockchainType
import com.example.jooq.tables.pojos.Currency
import jakarta.annotation.PostConstruct
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class CurrencyService(
    private val currencyRepo: CurrencyRepo,
    private val dslContext: DSLContext,
) {
    /**
     * список всех доступных валют храним в памяти чтобы не ходить каждый раз в базу
     */
    val allList = ArrayList<Currency>()

    val mapByName = HashMap<String, Currency>()
    val mapById = HashMap<Int, Currency>()
    val mapByBlockchain = HashMap<BlockchainType, MutableList<Currency>>()

    @PostConstruct
    fun init() {
        reload()
    }

    fun getById(id: Int): Currency {
        // TODO !!! блокировка получения значений на время релоада
        return mapById[id] ?: throw CurrencyNotFoundException()
    }

    fun reload() {
        // TODO !!! блокировка получения значений на время релоада
        mapByName.clear()
        mapById.clear()
        mapByBlockchain.clear()
        allList.clear()
        currencyRepo.loadAll(dslContext)
            .filter { it.enabled }
            .doOnNext {
                mapByName[it.name] = it
                mapById[it.id] = it
                var list = mapByBlockchain[it.blockchain]
                if (list == null) {
                    list = ArrayList()
                    list.add(it)
                    mapByBlockchain[it.blockchain] = list
                } else {
                    list.add(it)
                }
                allList.add(it)
            }.blockLast()
    }

    fun getByName(name: String): Currency {
        // TODO: воткнуть блокировку во время ожидания reload (асинхронно)
        val currency = mapByName[name]
        return currency ?: throw CurrencyNotFoundException(name)
    }

    fun getByBlockchain(type: BlockchainType): List<Currency> {
        // TODO: воткнуть блокировку во время ожидания reload (асинхронно)
        val currency = mapByBlockchain[type]
        return currency ?: listOf()
    }
}