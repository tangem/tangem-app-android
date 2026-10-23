package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class MoneyAccountAddFundsOptionsUM(
    val items: ImmutableList<MoneyAccountAddFundsOptionUM>,
)

@Immutable
internal data class MoneyAccountAddFundsOptionUM(
    val icon: TangemIconUM,
    val title: TextReference,
    val description: TextReference,
    val onClick: (() -> Unit)?,
    val isLoading: Boolean = false,
)