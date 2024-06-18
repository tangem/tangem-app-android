package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json

data class StakingTransactionDTO(
    @Json(name = "id")
    val id: String,
    @Json(name = "network")
    val network: TokenDTO.NetworkTypeDTO,
    @Json(name = "status")
    val status: StakingTransactionStatusDTO,
    @Json(name = "type")
    val type: StakingTransactionTypeDTO,
    @Json(name = "hash")
    val hash: String?,
    @Json(name = "signedTransaction")
    val signedTransaction: String?,
    @Json(name = "unsignedTransaction")
    val unsignedTransaction: String?,
    @Json(name = "stepIndex")
    val stepIndex: Int,
    @Json(name = "error")
    val error: String?,
    @Json(name = "gasEstimate")
    val gasEstimate: GasEstimateDTO?,
    @Json(name = "stakeId")
    val stakeId: String?,
    @Json(name = "explorerUrl")
    val explorerUrl: String?,
    @Json(name = "ledgerHwAppId")
    val ledgerHwAppId: String?,
    @Json(name = "isMessage")
    val isMessage: Boolean,
)
