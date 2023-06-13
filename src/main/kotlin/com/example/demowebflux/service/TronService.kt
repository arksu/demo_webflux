package com.example.demowebflux.service

import com.example.demowebflux.crypto.Base58
import com.example.demowebflux.exception.TronErrorException
import com.example.demowebflux.service.dto.TransactionTRC20
import com.example.demowebflux.service.dto.TransactionsTRC20Response
import com.example.demowebflux.util.randomHexStringByKotlinRandom
import com.example.jooq.enums.BlockchainType
import jakarta.annotation.PostConstruct
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


@Service
class TronService(
    private val webClient: WebClient
) {
    private val limit = 50

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

    /**
     * получаем все транзакции с адреса с лимитом, но в результате выдаем только USDT!
     */
    fun getUsdtTransactionsByAccount(address: String, blockchain: BlockchainType): Flux<TransactionTRC20> {
        val url = trongridUrl[blockchain] ?: throw TronErrorException("no url for $blockchain")
        val usdt = usdtAddress[blockchain] ?: throw TronErrorException("no USDT address for $blockchain")
        return webClient.get()
            .uri("$url/v1/accounts/{address}/transactions/trc20?limit=$limit", address)
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
            .uri("$url/v1/accounts/{address}/transactions/trc20?limit=$limit&only_confirmed=true", address)
            .retrieve()
            .bodyToMono(TransactionsTRC20Response::class.java)
            .flatMapMany { response ->
                Flux.fromIterable(response.data)
                    .filter {
                        it.token_info?.address == usdt
                    }
            }
    }


    private val PARAMS = SECNamedCurves.getByName("secp256k1")

    private val CURVE = ECDomainParameters(PARAMS.curve, PARAMS.g, PARAMS.n, PARAMS.h)

    private val DIGEST_SHA256 = MessageDigest.getInstance("SHA-256")

    fun generateWallet(): Pair<String, String> {
        val key = randomHexStringByKotlinRandom(64)
        val addressBytes = getTronAddress(key)
        val address = encodeBase58WithChecksum(addressBytes)
        return Pair(key, address)
    }

    fun getTronAddress(key: String): ByteArray {
        val privateKeyBigInt = BigInteger(key, 16)

        val pub = CURVE.g.multiply(privateKeyBigInt)
        val pubEncoded = pub.getEncoded(false)
        val pubBytes = Arrays.copyOfRange(pubEncoded, 1, pubEncoded.size)

        return sha3omit12(pubBytes)
    }

    fun sha3omit12(input: ByteArray): ByteArray {
        val hash = hashKeccak256(input)
        val address = hash.copyOfRange(11, hash.size)
        address[0] = 0x41
        return address
    }

    fun hashKeccak256(data: ByteArray): ByteArray {
        val keccak = Keccak.Digest256()
        keccak.update(data)
        return keccak.digest()
    }

    fun encodeBase58WithChecksum(data: ByteArray): String {
        val checksum = DIGEST_SHA256.digest(DIGEST_SHA256.digest(data))
        val dataWithChecksum = data + checksum.copyOfRange(0, 4)
        return Base58.encode(dataWithChecksum)
    }
}