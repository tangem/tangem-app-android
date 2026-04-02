package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class TokenDetailsBalanceBlockUM {

    abstract val actionButtons: ImmutableList<TangemButtonUM>
    abstract val tokenBalanceTypeUM: TokenBalanceTypeUM
    abstract val currencyIconState: CurrencyIconState

    data class Loading(
        override val actionButtons: ImmutableList<TangemButtonUM>,
        override val tokenBalanceTypeUM: TokenBalanceTypeUM,
        override val currencyIconState: CurrencyIconState,
    ) : TokenDetailsBalanceBlockUM()

    data class Content(
        override val actionButtons: ImmutableList<TangemButtonUM>,
        override val tokenBalanceTypeUM: TokenBalanceTypeUM,
        override val currencyIconState: CurrencyIconState,
        val displayCryptoBalance: TextReference,
        val displayFiatBalance: TextReference,
        val isBalanceFlickering: Boolean,
    ) : TokenDetailsBalanceBlockUM()

    data class Error(
        override val actionButtons: ImmutableList<TangemButtonUM>,
        override val tokenBalanceTypeUM: TokenBalanceTypeUM,
        override val currencyIconState: CurrencyIconState,
    ) : TokenDetailsBalanceBlockUM()

    fun copyActionButtons(buttons: ImmutableList<TangemButtonUM>): TokenDetailsBalanceBlockUM {
        return when (this) {
            is Content -> this.copy(actionButtons = buttons)
            is Error -> this.copy(actionButtons = buttons)
            is Loading -> this.copy(actionButtons = buttons)
        }
    }
}

internal sealed class TokenBalanceTypeUM {

    abstract val type: Type

    data object Single : TokenBalanceTypeUM() {
        override val type = Type.ALL
    }

    data class Multiple(
        override val type: Type,
        val availableTypes: ImmutableList<Type>,
        val onSelect: (Type) -> Unit,
    ) : TokenBalanceTypeUM()

    enum class Type {
        ALL,
        AVAILABLE,
    }
}