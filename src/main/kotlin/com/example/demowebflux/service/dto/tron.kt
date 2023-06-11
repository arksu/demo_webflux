package com.example.demowebflux.service.dto

data class TransferContract(
    val contractAddress: String,
    val ownerAddress: String,
    val toAddress: String,
    val amount: Long,
)

data class RawData(
    val contract: List<Contract>
)

data class TokenInfo(
    val symbol: String,
    val address: String,
    val decimals: Int,
    val name: String
)

data class Contract(
    val parameter: TransferContract,
    val type: String
)

data class TransactionTRC20(
    val transaction_id: String,
    val token_info: TokenInfo?,
    val block_timestamp: Long,
    val from: String,
    val to: String,
    val type: String,
    val value: String
)

data class TransactionsTRC20Response(
    val data: List<TransactionTRC20>,
    val success: Boolean
)