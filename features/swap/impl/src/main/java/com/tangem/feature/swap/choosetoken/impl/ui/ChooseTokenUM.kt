package com.tangem.feature.swap.choosetoken.impl.ui

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import kotlinx.collections.immutable.ImmutableList

internal data class ChooseTokenFullUM(
    val initialUM: ChooseTokenInitialUM,
    val contentUM: ChooseTokenUM?,
)

internal data class ChooseTokenUM(
    val walletList: WalletListUM,
    val isBalanceHidden: Boolean,
    val isSearching: Boolean,
    val tokensListData: TokenListUMData,
    val marketsState: SwapMarketState?,
)

internal data class ChooseTokenInitialUM(
    val screenTitle: TextReference,
    val onCloseClick: () -> Unit,
    val searchBar: SearchBarUM,
)

internal data class WalletListUM(
    val items: ImmutableList<TangemButtonUM>,
)