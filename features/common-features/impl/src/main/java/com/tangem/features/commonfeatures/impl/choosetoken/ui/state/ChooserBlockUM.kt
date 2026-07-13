package com.tangem.features.commonfeatures.impl.choosetoken.ui.state

import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokensUM

/**
 * The "add a token" block shown below the portfolio. Exactly one variant is active per chooser,
 * decided by [com.tangem.features.commonfeatures.api.choosetoken.ChooserBlock].
 */
internal sealed interface ChooserBlockUM {

    data class Market(val state: SwapMarketState) : ChooserBlockUM

    data class Predefined(val state: PredefinedTokensUM) : ChooserBlockUM
}