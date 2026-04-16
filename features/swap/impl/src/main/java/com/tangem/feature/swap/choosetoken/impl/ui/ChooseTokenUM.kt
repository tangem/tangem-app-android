package com.tangem.feature.swap.choosetoken.impl.ui

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.choosetoken.api.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.feature.swap.models.market.state.SwapMarketState

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