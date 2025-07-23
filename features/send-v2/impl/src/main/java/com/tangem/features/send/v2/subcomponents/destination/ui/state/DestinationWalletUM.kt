package com.tangem.features.send.v2.subcomponents.destination.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Available wallet to send
 *
 * @property name wallet name
 * @property userWalletId wallet id
 * @property address blockchain address
 * @property cryptoCurrency selected crypto currency
 */
@Immutable
data class DestinationWalletUM(
    val name: String,
    val userWalletId: UserWalletId,
    val address: String,
    val cryptoCurrency: CryptoCurrency,
)