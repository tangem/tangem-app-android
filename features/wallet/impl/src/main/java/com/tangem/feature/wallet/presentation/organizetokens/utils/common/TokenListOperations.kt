package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.tokenlist.TokenList

internal fun TokenList.disableSortingByBalance(): TokenList {
    return when (this) {
        is TokenList.GroupedByNetwork -> this.copy(sortedBy = TokensSortType.NONE)
        is TokenList.Ungrouped -> this.copy(sortedBy = TokensSortType.NONE)
        is TokenList.Empty -> this
    }
}