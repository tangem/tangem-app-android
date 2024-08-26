package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class MyPortfolioUM {

    data class Tokens(
        val tokens: ImmutableList<PortfolioTokenUM>,
        val buttonState: AddButtonState,
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM() {

        enum class AddButtonState {
            Loading,
            Available,
            Unavailable,
        }
    }

    data class AddFirstToken(
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM()

    data object Loading : MyPortfolioUM()

    data object Unavailable : MyPortfolioUM()
}