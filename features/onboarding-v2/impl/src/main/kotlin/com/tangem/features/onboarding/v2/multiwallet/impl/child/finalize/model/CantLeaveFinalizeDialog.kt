package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.onboarding.v2.impl.R

internal val CantLeaveFinalizeDialog: DialogMessage = DialogMessage(
    message = resourceReference(R.string.onboarding_backup_exit_warning),
    firstAction = EventMessageAction(
        title = resourceReference(R.string.warning_button_ok),
        onClick = {},
    ),
)