package com.tangem.features.txhistory.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
/** @property isSending the operation has collected its signatures and is on its way to the network */
data class JointCosignRequestsUM(
    val items: ImmutableList<TransactionItemUM>,
    val isBalanceHidden: Boolean,
    val isSending: Boolean = false,
) {

    companion object {
        val Empty = JointCosignRequestsUM(items = persistentListOf(), isBalanceHidden = false)
    }
}