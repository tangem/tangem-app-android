package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SolanaTransactionResponse(
    @Json(name = "result") val result: SolanaTransactionResult,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionResult(
    @Json(name = "validation") val validation: SolanaTransactionValidation,
    @Json(name = "simulation") val simulation: SolanaTransactionSimulation? = null,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionValidation(
    @Json(name = "result_type") val resultType: String,
    @Json(name = "description") val description: String?,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionSimulation(
    @Json(name = "account_summary") val accountSummary: SolanaTransactionAccountSummary,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_details") val errorDetails: String? = null,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionAccountSummary(
    @Json(name = "account_assets_diff")
    val accountAssetsDiff: List<SolanaTransactionAssetDiff>,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionAssetDiff(
    @Json(name = "asset_type") val assetType: String,
    @Json(name = "asset") val asset: SolanaTransactionAsset,
    @Json(name = "in") val inTransfer: SolanaTransferDetail? = null,
    @Json(name = "out") val outTransfer: SolanaTransferDetail? = null,
)

@JsonClass(generateAdapter = true)
data class SolanaTransactionAsset(
    @Json(name = "address") val address: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "decimals") val decimals: Int? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "logo") val logoUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SolanaTransferDetail(
    @Json(name = "value") val amount: String? = null,
    @Json(name = "summary") val summary: String? = null,
)