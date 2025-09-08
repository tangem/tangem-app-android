package com.tangem.features.yieldlending.impl.main.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class YieldLendingUM {

    data object Initial : YieldLendingUM()

    data object Loading : YieldLendingUM()

    data object Content : YieldLendingUM()
}