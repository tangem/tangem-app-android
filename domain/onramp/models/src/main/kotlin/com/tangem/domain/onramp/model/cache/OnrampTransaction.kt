package com.tangem.domain.onramp.model.cache

import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
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
 * @property paymentMethod      Transaction payment method
 * @property paymentMethod      Residency country
 */
data class OnrampTransaction(
    val txId: String,
    val userWalletId: UserWalletId,
    val fromAmount: SerializedBigDecimal,
    val fromCurrency: OnrampCurrency,
    val toAmount: SerializedBigDecimal,
    val toCurrencyId: String,
    val status: OnrampStatus.Status,
    val externalTxUrl: String?,
    val externalTxId: String?,
    val timestamp: Long,
    val providerName: String, // todo onramp fix after SwapProvider moved to own module
    val providerImageUrl: String, // todo onramp fix after SwapProvider moved to own module
    val providerType: String, // todo onramp fix after SwapProvider moved to own module
    val redirectUrl: String,
    val paymentMethod: String,
    val residency: String,
)