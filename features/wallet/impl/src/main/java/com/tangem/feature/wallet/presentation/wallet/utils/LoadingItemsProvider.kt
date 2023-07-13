package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState.MultiCurrencyItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal object LoadingItemsProvider {

    fun getLoadingMultiCurrencyTokens(): PersistentList<MultiCurrencyItem> {
        return List(size = 5) { TokenItemState.Loading }
            .map { MultiCurrencyItem.Token(it) }
            .toPersistentList()
    }
}