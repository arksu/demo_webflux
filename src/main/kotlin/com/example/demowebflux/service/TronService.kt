package com.example.demowebflux.service

import com.example.demowebflux.exception.TronErrorException
import com.example.demowebflux.service.dto.TransactionTRC20
import com.example.demowebflux.service.dto.TransactionsTRC20Response
import com.example.jooq.enums.BlockchainType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class TronService(
    private val webClient: WebClient
) {

    /**
     * адреса USDT контрактов в разных БЧ (нужно для тестирования)
     */
    val usdtAddress: Map<BlockchainType, String> = mapOf(
        BlockchainType.TRON to "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
        BlockchainType.TRON_NILE to "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj",
        // TODO shasta
    )

    /**
     * адреса апи trongrid по блокчейну
     */
    val trongridUrl: Map<BlockchainType, String> = mapOf(
        BlockchainType.TRON to "https://api.trongrid.io",
        BlockchainType.TRON_NILE to "https://nile.trongrid.io",
        BlockchainType.TRON_SHASTA to "https://api.shasta.trongrid.io",
    )

    fun getUsdtTransactionsByAccount(address: String, blockchain: BlockchainType): Flux<TransactionTRC20> {
        val url = trongridUrl[blockchain] ?: throw TronErrorException("no url for $blockchain")
        val usdt = usdtAddress[blockchain] ?: throw TronErrorException("no USDT address for $blockchain")
        return webClient.get()
            .uri("$url/v1/accounts/{address}/transactions/trc20", address)
            .retrieve()
            .bodyToMono(TransactionsTRC20Response::class.java)
            .flatMapMany { response ->
                Flux.fromIterable(response.data)
                    .filter {
                        it.token_info?.address == usdt
                    }
            }
    }

    fun getUsdtConfirmedTransactionsByAccount(address: String, blockchain: BlockchainType): Flux<TransactionTRC20> {
        val url = trongridUrl[blockchain] ?: throw TronErrorException("no url for $blockchain")
        val usdt = usdtAddress[blockchain] ?: throw TronErrorException("no USDT address for $blockchain")
        return webClient.get()
            .uri("$url/v1/accounts/{address}/transactions/trc20?only_confirmed=true", address)
            .retrieve()
            .bodyToMono(TransactionsTRC20Response::class.java)
            .flatMapMany { response ->
                Flux.fromIterable(response.data)
                    .filter {
                        it.token_info?.address == usdt
                    }
            }
    }
}