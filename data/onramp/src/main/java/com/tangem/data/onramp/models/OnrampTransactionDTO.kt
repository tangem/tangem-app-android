package com.tangem.data.onramp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.onramp.models.response.Status
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Model for local storing onramp transaction
 *
 * @property txId               Inner express tx id
 * @property userWalletId       Wallet where tx was performed
 * @property fromAmount         Onramp amount to buy crypto
 * @property fromCurrency       Onramp currency
 * @property toAmount           Receiving crypto amount
 * @property toCurrencyId       Receiving crypto currency id [CryptoCurrency.ID.value]
 * @property status             Transaction status
 * @property externalTxUrl      Link to transaction on provider side
 * @property externalTxId       Id of transaction on provider side
 * @property timestamp          Transaction create time
 * @property providerName       Provider name
 * @property providerImageUrl   Provider image link
 * @property providerType       Provider type
 */
@JsonClass(generateAdapter = true)
data class OnrampTransactionDTO(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "userWalletId")
    val userWalletId: UserWalletId,
    @Json(name = "fromAmount")
    val fromAmount: SerializedBigDecimal,
    @Json(name = "fromCurrency")
    val fromCurrency: OnrampCurrencyDTO,
    @Json(name = "toAmount")
    val toAmount: SerializedBigDecimal,
    @Json(name = "toCurrencyId")
    val toCurrencyId: String,
    @Json(name = "status")
    val status: Status,
    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,
    @Json(name = "externalTxId")
    val externalTxId: String?,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "providerName")
    val providerName: String, // todo onramp fix after SwapProvider moved to own module
    @Json(name = "providerImageUrl")
    val providerImageUrl: String, // todo onramp fix after SwapProvider moved to own module
    @Json(name = "providerType")
    val providerType: String, // todo onramp fix after SwapProvider moved to own module
    @Json(name = "paymentMethod")
    val paymentMethod: String,
    @Json(name = "residency")
    val residency: String,
)