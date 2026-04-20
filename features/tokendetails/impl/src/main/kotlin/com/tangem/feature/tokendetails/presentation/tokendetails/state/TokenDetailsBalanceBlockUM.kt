package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tokendetails.impl.R
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
        val displayCryptoBalanceAll: TextReference,
        val displayFiatBalanceAll: TextReference,
        val displayCryptoBalanceAvailable: TextReference?,
        val displayFiatBalanceAvailable: TextReference?,
        val isBalanceFlickering: Boolean,
    ) : TokenDetailsBalanceBlockUM() {

        val displayCryptoBalance: TextReference
            get() = when (tokenBalanceTypeUM.type) {
                TokenBalanceTypeUM.Type.ALL -> displayCryptoBalanceAll
                TokenBalanceTypeUM.Type.AVAILABLE -> displayCryptoBalanceAvailable ?: displayCryptoBalanceAll
            }

        val displayFiatBalance: TextReference
            get() = when (tokenBalanceTypeUM.type) {
                TokenBalanceTypeUM.Type.ALL -> displayFiatBalanceAll
                TokenBalanceTypeUM.Type.AVAILABLE -> displayFiatBalanceAvailable ?: displayFiatBalanceAll
            }
    }

    data class Error(
        override val actionButtons: ImmutableList<TangemButtonUM>,
        override val tokenBalanceTypeUM: TokenBalanceTypeUM,
        override val currencyIconState: CurrencyIconState,
    ) : TokenDetailsBalanceBlockUM()

    fun copyCurrencyIconState(iconState: CurrencyIconState): TokenDetailsBalanceBlockUM {
        return when (this) {
            is Content -> this.copy(currencyIconState = iconState)
            is Error -> this.copy(currencyIconState = iconState)
            is Loading -> this.copy(currencyIconState = iconState)
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
        val onSelect: () -> Unit,
    ) : TokenBalanceTypeUM()

    enum class Type(val text: TextReference) {
        ALL(resourceReference(R.string.token_details_balance_total)),
        AVAILABLE(resourceReference(R.string.token_details_balance_available)),
    }
}