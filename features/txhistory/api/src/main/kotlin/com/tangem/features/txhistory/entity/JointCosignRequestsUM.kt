package com.tangem.features.txhistory.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class JointCosignRequestsUM(
    val items: ImmutableList<TransactionItemUM>,
    val isBalanceHidden: Boolean,
) {

    companion object {
        val Empty = JointCosignRequestsUM(items = persistentListOf(), isBalanceHidden = false)
    }
}