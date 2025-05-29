package com.tangem.features.send.impl.presentation.domain

import androidx.compose.runtime.Immutable
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Available wallet to send
 *
 * @property name wallet name
 * @property userWalletId wallet id
 * @property address blockchain address
 */
@Immutable
data class AvailableWallet(
    val name: String,
    val userWalletId: UserWalletId,
    val address: String,
    val cryptoCurrency: CryptoCurrency,
)