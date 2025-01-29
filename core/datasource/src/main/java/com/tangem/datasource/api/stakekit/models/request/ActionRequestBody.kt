package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.request.ConstructTransactionRequestBody.GasArgs
import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.domain.staking.model.stakekit.action.StakingActionType

@JsonClass(generateAdapter = true)
data class PendingActionRequestBody(
    @Json(name = "type")
    val type: StakingActionType,
    @Json(name = "integrationId")
    val integrationId: String,
    @Json(name = "passthrough")
    val passthrough: String,
    @Json(name = "args")
    val args: ActionRequestBodyArgs,
    @Json(name = "gasArgs")
    val gasArgs: GasArgs? = null, // used only in estimate_gas request
)

@JsonClass(generateAdapter = true)
data class ActionRequestBody(
    @Json(name = "integrationId")
    val integrationId: String,
    @Json(name = "addresses")
    val addresses: Address,
    @Json(name = "args")
    val args: ActionRequestBodyArgs,
    @Json(name = "referralCode")
    val referralCode: String? = null,
    @Json(name = "gasArgs")
    val gasArgs: GasArgs? = null, // used only in estimate_gas request
)

@JsonClass(generateAdapter = true)
data class ActionRequestBodyArgs(
    @Json(name = "amount")
    val amount: String,
    @Json(name = "validatorAddress")
    val validatorAddress: String? = null,
    @Json(name = "validatorAddresses")
    val validatorAddresses: List<String>? = null,
    @Json(name = "providerId")
    val providerId: String? = null,
    @Json(name = "duration")
    val duration: String? = null,
    @Json(name = "nfts")
    val nfts: List<BalanceDTO.PendingAction.PendingActionArgs.Nft>? = null,
    @Json(name = "ledgerWalletAPICompatible")
    val ledgerWalletAPICompatible: Boolean? = null,
    @Json(name = "tronResource")
    val tronResource: TronResource? = null,
    @Json(name = "signatureVerification")
    val signatureVerification: SignatureVerification? = null,
    @Json(name = "inputToken")
    val inputToken: TokenDTO? = null,
)

@JsonClass(generateAdapter = true)
data class SignatureVerification(
    @Json(name = "message")
    val message: String,
    @Json(name = "signed")
    val signed: String,
)

@JsonClass(generateAdapter = false)
enum class TronResource {
    @Json(name = "ENERGY")
    ENERGY,

    @Json(name = "BANDWIDTH")
    BANDWIDTH,
}