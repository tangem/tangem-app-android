package com.tangem.features.commonfeatures.impl.choosetoken.ui

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState

internal data class ChooseTokenFullUM(
    val initialUM: ChooseTokenInitialUM,
    val portfolioBlock: ChooseTokenPortfolioFullBlockUM?,
    val marketsBlock: SwapMarketState?,
)

internal data class ChooseTokenInitialUM(
    val screenTitle: TextReference,
    val onCloseClick: () -> Unit,
    val searchBar: SearchBarUM,
)