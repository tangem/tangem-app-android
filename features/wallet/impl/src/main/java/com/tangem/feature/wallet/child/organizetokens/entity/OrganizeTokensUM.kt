package com.tangem.feature.wallet.child.organizetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.event.StateEvent
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class OrganizeTokensUM(
    val tokenList: PersistentList<OrganizeRowItemUM>,
    val organizeMenuUM: OrganizeMenuUM,
    val isGrouped: Boolean,
    val isAccountsMode: Boolean,
    val scrollListToTop: StateEvent<Unit>,
    val cancelButton: TangemButtonUM,
    val applyButton: TangemButtonUM,
    val isBalanceHidden: Boolean,
) {

    data class OrganizeMenuUM(
        val isEnabled: Boolean = false,
        val isSortedByBalance: Boolean = false,
        val isGrouped: Boolean = false,
        val onSortClick: () -> Unit,
        val onGroupClick: () -> Unit,
    )
}