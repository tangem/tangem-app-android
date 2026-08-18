package com.tangem.features.jointaccount.join.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Mutable accumulator of the join flow, shared between the step models.
 *
 * Owned by [JointAccountJoinModel], so it lives exactly as long as the flow and survives configuration
 * changes with it. Steps write their chunk on Continue and read it back to restore their state after being

 */
internal class JointAccountJoinDraftHolder {

    val draft: StateFlow<JointAccountJoinDraft>
        field = MutableStateFlow(JointAccountJoinDraft())

    fun setSelectedWallet(walletId: UserWalletId) {
        draft.update { it.copy(selectedWalletId = walletId) }
    }

    fun setDisplayName(displayName: String) {
        draft.update { it.copy(displayName = displayName) }
    }
}