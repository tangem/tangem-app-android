package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ManageTokensUM(
    val popBack: () -> Unit,
    val items: ImmutableList<CurrencyItemUM>,
)