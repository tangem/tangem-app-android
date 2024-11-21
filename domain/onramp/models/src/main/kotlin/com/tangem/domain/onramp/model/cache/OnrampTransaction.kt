package com.tangem.domain.onramp.model.cache

import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class OnrampTransaction(
    val txId: String,
    val userWalletId: UserWalletId,
    val fromAmount: SerializedBigDecimal,
    val fromCurrency: OnrampCurrency,
    val toAmount: SerializedBigDecimal,
    val toCurrencyId: String,
    val providerName: String,
    val providerImageUrl: String,
)
