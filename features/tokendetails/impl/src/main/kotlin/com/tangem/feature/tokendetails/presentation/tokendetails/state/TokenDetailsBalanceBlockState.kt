package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

internal sealed class TokenDetailsBalanceBlockState {

    abstract val actionButtons: ImmutableList<TokenDetailsActionButton>
    abstract val balanceSegmentedButtonConfig: ImmutableList<TokenBalanceSegmentedButtonConfig>
    abstract val selectedBalanceType: BalanceType

    data class Loading(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
        override val balanceSegmentedButtonConfig: ImmutableList<TokenBalanceSegmentedButtonConfig>,
        override val selectedBalanceType: BalanceType,
    ) : TokenDetailsBalanceBlockState()

    data class Content(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
        override val balanceSegmentedButtonConfig: ImmutableList<TokenBalanceSegmentedButtonConfig>,
        override val selectedBalanceType: BalanceType,
        val fiatBalance: BigDecimal?,
        val cryptoBalance: BigDecimal?,
        val onBalanceSelect: (TokenBalanceSegmentedButtonConfig) -> Unit,
        val displayCryptoBalance: String,
        val displayFiatBalance: String,
        val isBalanceSelectorEnabled: Boolean,
        val isBalanceFlickering: Boolean,
    ) : TokenDetailsBalanceBlockState()

    data class Error(
        override val actionButtons: ImmutableList<TokenDetailsActionButton>,
        override val balanceSegmentedButtonConfig: ImmutableList<TokenBalanceSegmentedButtonConfig>,
        override val selectedBalanceType: BalanceType,
    ) : TokenDetailsBalanceBlockState()

    fun copyActionButtons(buttons: ImmutableList<TokenDetailsActionButton>): TokenDetailsBalanceBlockState {
        return when (this) {
            is Content -> this.copy(actionButtons = buttons)
            is Error -> this.copy(actionButtons = buttons)
            is Loading -> this.copy(actionButtons = buttons)
        }
    }
}