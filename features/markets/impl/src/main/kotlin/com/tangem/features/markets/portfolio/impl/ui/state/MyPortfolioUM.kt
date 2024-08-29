package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface MyPortfolioUM {

    sealed interface AddTokensAvailabilityUM : MyPortfolioUM {
        val bsConfig: TangemBottomSheetConfig
        val onAddClick: () -> Unit

        fun copySealed(bsConfig: TangemBottomSheetConfig): AddTokensAvailabilityUM {
            return when (this) {
                is AddFirstToken -> copy(bsConfig = bsConfig)
                is Tokens -> copy(bsConfig = bsConfig)
            }
        }
    }

    data class Tokens(
        val tokens: ImmutableList<PortfolioTokenUM>,
        val buttonState: AddButtonState,
        override val bsConfig: TangemBottomSheetConfig,
        override val onAddClick: () -> Unit,
    ) : AddTokensAvailabilityUM {

        enum class AddButtonState {
            Loading,
            Available,
            Unavailable,
        }
    }

    data class AddFirstToken(
        override val bsConfig: TangemBottomSheetConfig,
        override val onAddClick: () -> Unit,
    ) : AddTokensAvailabilityUM

    data object Loading : MyPortfolioUM

    data object Unavailable : MyPortfolioUM
}