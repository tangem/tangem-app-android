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
    val referralCode: String? = null,
) {

    data class EnterActionRequestBodyArgs(
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
        val nfts: List<YieldBalanceWrapperDTO.BalanceDTO.PendingAction.PendingActionArgs.Nft>? = null,
        @Json(name = "ledgerWalletAPICompatible")
        val ledgerWalletAPICompatible: Boolean? = null,
        @Json(name = "tronResource")
        val tronResource: String? = null,
        @Json(name = "signatureVerification")
        val signatureVerification: SignatureVerification? = null,
        @Json(name = "inputToken")
        val inputToken: TokenDTO? = null,
    )

    data class SignatureVerification(
        @Json(name = "message")
        val message: String,
        @Json(name = "signed")
        val signed: String,
    )
}