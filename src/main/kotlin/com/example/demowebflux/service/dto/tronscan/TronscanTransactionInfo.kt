package com.example.demowebflux.service.dto.tronscan

data class TronscanTransactionInfo(
    val block: Int,
    val hash: String,
    val timestamp: Long,
    val ownerAddress: String,
    val contract_type: String,
    val toAddress: String,

    val confirmations: Int,
    val confirmed: Boolean,
    val revert: Boolean,
    val contractRet: String?,
    val contractData: ContractData,
    val cost: Cost,
    val trigger_info: TriggerInfo,
    val srConfirmList: List<SrConfirm>,
    val contractInfo: Map<String, ContractInfo>?,
    val normalAddressInfo: Map<String, NormalAddressInfo>?,
    val contract_map: Map<String, Boolean>?,
    val riskTransaction: Boolean,

    val contractType: Int?,
    val event_count: Int?,
    val fee_limit: Int?,
    val tokenTransferInfo: TokenTransferInfo?,
    val transfersAllList: List<TransfersAll>?,
    val trc20TransferInfo: List<Trc20TransferInfo>?,
    val triggerContractType: Int?,
)