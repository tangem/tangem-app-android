package com.tangem.domain.staking.model.transaction

import com.tangem.domain.staking.model.NetworkType

data class StakingTransaction(
    val id: String,
    val network: NetworkType,
    val status: StakingTransactionStatus,
    val type: StakingTransactionType,
    val hash: String?,
    val signedTransaction: String?,
    val unsignedTransaction: String?,
    val stepIndex: Int,
    val error: String?,
    val gasEstimate: GasEstimate?,
    val stakeId: String?,
    val explorerUrl: String?,
    val ledgerHwAppId: String?,
    val isMessage: Boolean,
)
