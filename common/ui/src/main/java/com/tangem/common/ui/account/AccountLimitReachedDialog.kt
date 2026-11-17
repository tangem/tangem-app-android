package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction

fun accountLimitReachedDialog(limit: Int): DialogMessage = DialogMessage(
    title = resourceReference(R.string.account_add_limit_dialog_title),
    message = resourceReference(
        id = R.string.account_add_limit_dialog_description,
        formatArgs = wrappedList(limit.toString()),
    ),
    firstActionBuilder = {
        EventMessageAction(title = resourceReference(R.string.common_got_it), onClick = {})
    },
)