package com.tangem.feature.wallet.child.organizetokens.ui

import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM

internal fun isValidDropTarget(
    item: OrganizeRowItemUM,
    dragging: OrganizeRowItemUM?,
    isGrouped: Boolean,
    isAccountsMode: Boolean,
): Boolean {
    return when {
        dragging == null -> true
        item.id == dragging.id -> true
        dragging is OrganizeRowItemUM.Network ->
            item is OrganizeRowItemUM.Placeholder && item.accountId == dragging.accountId
        dragging is OrganizeRowItemUM.Token -> when (item) {
            is OrganizeRowItemUM.Token -> when {
                isGrouped -> item.groupId == dragging.groupId
                isAccountsMode -> item.accountId == dragging.accountId
                else -> true
            }
            else -> false
        }
        else -> false
    }
}