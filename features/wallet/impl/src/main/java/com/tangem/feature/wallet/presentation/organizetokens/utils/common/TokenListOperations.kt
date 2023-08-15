package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenList.SortType

internal fun TokenList.updateSorting(isSortedByBalance: Boolean): TokenList {
    val sortType = if (isSortedByBalance) SortType.BALANCE else SortType.NONE

    return when (this) {
        is TokenList.GroupedByNetwork -> this.copy(sortedBy = sortType)
        is TokenList.Ungrouped -> this.copy(sortedBy = sortType)
        is TokenList.NotInitialized -> this
    }
}
