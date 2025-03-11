package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class MyPortfolioUM {

    abstract val addToPortfolioBSConfig: TangemBottomSheetConfig?

    data class Tokens(
        override val addToPortfolioBSConfig: TangemBottomSheetConfig,
        val tokens: ImmutableList<PortfolioTokenUM>,
        val buttonState: AddButtonState,
        val tokenReceiveBSConfig: TangemBottomSheetConfig,
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM() {

        enum class AddButtonState {
            Loading,
            Available,
            Unavailable,
        }
    }

    data class AddFirstToken(
        override val addToPortfolioBSConfig: TangemBottomSheetConfig,
        val onAddClick: () -> Unit,
    ) : MyPortfolioUM()

    data object Loading : MyPortfolioUM() {
        override val addToPortfolioBSConfig: TangemBottomSheetConfig? = null
    }

    data object Unavailable : MyPortfolioUM() {
        override val addToPortfolioBSConfig: TangemBottomSheetConfig? = null
    }

    data object UnavailableForWallet : MyPortfolioUM() {
        override val addToPortfolioBSConfig: TangemBottomSheetConfig? = null
    }
}