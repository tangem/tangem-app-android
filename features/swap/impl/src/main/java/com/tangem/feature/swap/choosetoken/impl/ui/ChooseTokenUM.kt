package com.tangem.feature.swap.choosetoken.impl.ui

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import kotlinx.collections.immutable.ImmutableList

internal data class ChooseTokenUM(
    val screenTitle: TextReference,
    val onCloseClick: () -> Unit,
    val walletList: WalletListUM,
    val searchBar: SearchBarUM,
    val isBalanceHidden: Boolean,
    val isAfterSearch: Boolean,
    val tokensListData: TokenListUMData,
    val marketsState: SwapMarketState?,
)

internal data class WalletListUM(
    val items: ImmutableList<TangemButtonUM>,
)