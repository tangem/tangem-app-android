package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenList.SortType

internal fun TokenList.disableSortingByBalance(): TokenList {
    return when (this) {
        is TokenList.GroupedByNetwork -> this.copy(sortedBy = SortType.NONE)
        is TokenList.Ungrouped -> this.copy(sortedBy = SortType.NONE)
        is TokenList.Empty -> this
    }
}