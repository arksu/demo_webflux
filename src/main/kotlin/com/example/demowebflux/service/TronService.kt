package com.example.demowebflux.service

import com.example.demowebflux.crypto.Base58
import com.example.demowebflux.exception.TronErrorException
import com.example.demowebflux.service.dto.TransactionTRC20
import com.example.demowebflux.service.dto.TransactionsTRC20Response
import com.example.demowebflux.service.dto.tronscan.TronscanTransactionInfo
import com.example.demowebflux.util.randomHexStringByKotlinRandom
import com.example.jooq.enums.BlockchainType
import com.example.jooq.tables.pojos.Currency
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*


@Service
class TronService(
    private val webClient: WebClient
) {
    private val limit = 50

    /**
     * адреса апи trongrid по блокчейну
     */
    val trongridUrl: Map<BlockchainType, String> = mapOf(
        BlockchainType.TRON to "https://api.trongrid.io",
        BlockchainType.TRON_NILE to "https://nile.trongrid.io",
        BlockchainType.TRON_SHASTA to "https://api.shasta.trongrid.io",
    )

    val tronscanUrl: Map<BlockchainType, String> = mapOf(
        BlockchainType.TRON_NILE to "https://nileapi.tronscan.org"
    )

    /**
     * получаем все транзакции с адреса с лимитом, но в результате выдаем только Token!
     */
    fun getTokenTransactionsByAccount(address: String, currency: Currency): Flux<TransactionTRC20> {
        val url = trongridUrl[currency.blockchain] ?: throw TronErrorException("no url for ${currency.blockchain}")
        val contractAddress = currency.contractAddress?.trim() ?: throw TronErrorException("no Token contract address for ${currency.contractAddress}")

        // https://developers.tron.network/v3.7/reference/trc20-transaction-information-by-account-address

        return webClient.get()
            .uri("$url/v1/accounts/{address}/transactions/trc20?contract_address=$contractAddress&limit=$limit", address)
            .retrieve()
            .bodyToMono(TransactionsTRC20Response::class.java)
            .flatMapMany { response ->
                Flux.fromIterable(response.data)
                    .filter {
                        // должен совпасть и адрес контракта и название токена
                        it.token_info?.address == contractAddress && currency.token.equals(it.token_info.symbol, ignoreCase = true)
                    }
            }
    }

    fun getTokenConfirmedTransactionsByAccount(address: String, currency: Currency): Flux<TransactionTRC20> {
        val url = trongridUrl[currency.blockchain] ?: throw TronErrorException("no url for ${currency.blockchain}")
        val contractAddress = currency.contractAddress?.trim() ?: throw TronErrorException("no Token contract address for ${currency.contractAddress}")
        return webClient.get()
            .uri("$url/v1/accounts/{address}/transactions/trc20?contract_address=$contractAddress&limit=$limit&only_confirmed=true", address)
            .retrieve()
            .bodyToMono(TransactionsTRC20Response::class.java)
            .flatMapMany { response ->
                Flux.fromIterable(response.data)
                    .filter {
                        it.token_info?.address == contractAddress
                    }
            }
    }

    fun getTronscanTransactionInfo(id: String, blockchain: BlockchainType): Mono<TronscanTransactionInfo> {
        val url = tronscanUrl[blockchain]
        return webClient.get()
            .uri("$url/api/transaction-info?hash=$id")
            .retrieve()
            .bodyToMono(TronscanTransactionInfo::class.java)
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