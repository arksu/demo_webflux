package com.example.demowebflux.service.dto.tronscan

data class TronscanTransactionInfo(
    val block: Int,
    val confirmations: Int,
    val confirmed: Boolean,
    val contractInfo: Map<String, ContractInfo>?,
    val contractData: ContractData?,
    val contractRet: String?,
    val contractType: Int?,
    val contract_map: Map<String, Boolean>?,
    val contract_type: String?,
    val cost: Cost,
    val event_count: Int,
    val fee_limit: Int,
    val hash: String,
    val normalAddressInfo: Map<String, NormalAddressInfo>?,
    val ownerAddress: String,
    val revert: Boolean,
    val riskTransaction: Boolean,
    val srConfirmList: List<SrConfirm>,
    val timestamp: Long,
    val toAddress: String,
    val tokenTransferInfo: TokenTransferInfo,
    val transfersAllList: List<TransfersAll>,
    val trc20TransferInfo: List<Trc20TransferInfo>,
    val triggerContractType: Int,
    val trigger_info: TriggerInfo
)