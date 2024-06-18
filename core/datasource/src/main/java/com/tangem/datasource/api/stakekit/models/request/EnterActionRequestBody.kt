package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO

data class EnterActionRequestBody(
    @Json(name = "integrationId")
    val integrationId: String,
    @Json(name = "addresses")
    val addresses: Address,
    @Json(name = "args")
    val args: EnterActionRequestBodyArgs,
    @Json(name = "referralCode")
    val referralCode: String,
) {

    data class EnterActionRequestBodyArgs(
        @Json(name = "amount")
        val amount: String,
        @Json(name = "validatorAddress")
        val validatorAddress: String?,
        @Json(name = "validatorAddresses")
        val validatorAddresses: List<String>?,
        @Json(name = "providerId")
        val providerId: String?,
        @Json(name = "duration")
        val duration: String?,
        @Json(name = "nfts")
        val nfts: List<YieldBalanceWrapperDTO.BalanceDTO.PendingAction.PendingActionArgs.Nft>?,
        @Json(name = "ledgerWalletAPICompatible")
        val ledgerWalletAPICompatible: Boolean?,
        @Json(name = "tronResource")
        val tronResource: String?,
        @Json(name = "signatureVerification")
        val signatureVerification: SignatureVerification?,
        @Json(name = "inputToken")
        val inputToken: TokenDTO?,
    )

    data class SignatureVerification(
        @Json(name = "message")
        val message: String,
        @Json(name = "signed")
        val signed: String,
    )
}
