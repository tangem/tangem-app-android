package com.tangem.features.feed.components.market.details.portfolio.impl.ui.state

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

    data class Content(
        val items: ImmutableList<PortfolioListItem>,
        val buttonState: Tokens.AddButtonState,
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM()

    data class AddFirstToken(
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM()

    data object Loading : MyPortfolioUM()

    data object Unavailable : MyPortfolioUM()

    data object UnavailableForWallet : MyPortfolioUM()
}