package com.tangem.features.commonfeatures.impl.choosetoken.ui

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.features.commonfeatures.impl.choosetoken.ui.state.ChooserBlockUM

internal data class ChooseTokenFullUM(
    val initialUM: ChooseTokenInitialUM,
    val portfolioBlock: ChooseTokenPortfolioFullBlockUM?,
    val chooserBlock: ChooserBlockUM? = null,
)

internal data class ChooseTokenInitialUM(
    val screenTitle: TextReference,
    val isAppBarShown: Boolean,
    val onCloseClick: () -> Unit,
    val searchBar: SearchBarUM,
)

internal val ChooseTokenFullUM.marketState: SwapMarketState?
    get() = (chooserBlock as? ChooserBlockUM.Market)?.state

internal val ChooseTokenFullUM.isNotFoundState: Boolean
    get() {
        if (portfolioBlock == null) return false
        val marketsBlock = marketState ?: return false
        return portfolioBlock.tokensListData.tokensList.isEmpty() &&
            portfolioBlock.isSearching &&
            marketsBlock !is SwapMarketState.Content &&
            marketsBlock !is SwapMarketState.Loading
    }

internal val ChooseTokenFullUM.isEmptyState: Boolean
    get() {
        if (portfolioBlock == null) return false
        val marketsBlock = marketState ?: return false
        return portfolioBlock.tokensListData.tokensList.isEmpty() &&
            !portfolioBlock.isSearching &&
            marketsBlock !is SwapMarketState.Content &&
            marketsBlock !is SwapMarketState.Loading
    }

/**
 * The "In your portfolio" title is shown only above something: tokens, wallet tabs or a placeholder.
 * It also carries the balance filter dropdown, so it stays whenever that filter exists — otherwise an
 * empty filtered result would leave no way to switch the filter back.
 */
internal val ChooseTokenFullUM.isPortfolioTitleShown: Boolean
    get() {
        val block = portfolioBlock ?: return false
        return block.balanceFilterUM != null ||
            block.walletList.items.isNotEmpty() ||
            block.tokensListData.tokensList.isNotEmpty() ||
            isEmptyState ||
            isNotFoundState
    }