package com.tangem.feature.wallet.presentation.ui.config

import androidx.annotation.DrawableRes
import com.tangem.feature.wallet.presentation.ui.state.TokenOptionsUIState
import com.tangem.feature.wallet.presentation.ui.state.TokenUIState

/**
 * Token config
 *
 * @property name of token/coin
 * @property amount of token
 * @property fiatAmount amount in fiat
 * @property priceChange value of price changing
 * @property iconUrl
 * @property icon token/coin icon
 * @property networkIcon
 * @property tokenUIState token state [TokenUIState] loading etc.
 * @property tokenOptionsUIState state for token options like 'unreachable', 'hidden' etc
 * @property hasPending pending tx in blockchain
 */
data class TokenV2Config(
    val name: String,
    val amount: String,
    val fiatAmount: String,
    val priceChange: PriceChange,
    val iconUrl: String?,
    @DrawableRes val icon: Int?,
    @DrawableRes val networkIcon: Int,
    val tokenUIState: TokenUIState,
    val tokenOptionsUIState: TokenOptionsUIState,
    val hasPending: Boolean,
)

data class PriceChange(
    val valuePercent: String,
    val type: PriceChangeType,
)

enum class PriceChangeType {
    UP, DOWN
}