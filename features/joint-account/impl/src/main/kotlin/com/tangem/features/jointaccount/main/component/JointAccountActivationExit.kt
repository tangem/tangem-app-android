package com.tangem.features.jointaccount.main.component

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.R as CoreUiR

internal class JointAccountActivationExit(
    private val messageSender: UiMessageSender,
    private val closeFlow: () -> Unit,
) {

    private var isExited = false

    fun exit() {
        if (isExited) return
        isExited = true

        messageSender.send(
            SnackbarMessage(
                message = resourceReference(CoreUiR.string.joint_account_activated_snackbar),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
        )
        closeFlow()
    }
}