package com.tangem.features.jointaccount.common.ui

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.jointaccount.impl.R

internal fun jointAccountAccessLossNote(threshold: Int, total: Int): TextReference {
    val membersToLoseAccess = total - threshold + 1

    return when {
        membersToLoseAccess >= total -> resourceReference(R.string.joint_account_members_access_loss_all)
        membersToLoseAccess <= 1 -> resourceReference(R.string.joint_account_members_access_loss_any)
        else -> resourceReference(
            id = R.string.joint_account_members_access_loss_some,
            formatArgs = wrappedList(membersToLoseAccess.toString(), total.toString()),
        )
    }
}