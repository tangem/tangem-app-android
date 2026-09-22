package com.tangem.features.txhistory.joint.operation.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.core.ui.R as CoreUiR

/**
 * The initiator deletes the operation for everyone, any other member only declines it — two different questions
 * before the same request.
 */
internal class JointOperationRefusalMessages(
    private val onConfirm: () -> Unit,
    private val onOperationGone: () -> Unit,
) {

    fun confirmation(isInitiator: Boolean): DialogMessage {
        val (title, message, action) = if (isInitiator) {
            Triple(
                CoreUiR.string.joint_operation_delete_alert_title,
                CoreUiR.string.joint_operation_delete_alert_message,
                CoreUiR.string.common_delete,
            )
        } else {
            Triple(
                CoreUiR.string.joint_operation_decline_alert_title,
                CoreUiR.string.joint_operation_decline_alert_message,
                CoreUiR.string.common_decline,
            )
        }

        return DialogMessage(
            title = resourceReference(title),
            message = resourceReference(message),
            firstActionBuilder = {
                EventMessageAction(title = resourceReference(action), isWarning = true, onClick = onConfirm)
            },
            secondActionBuilder = { cancelAction() },
        )
    }

    /**
     * An operation the backend has already lost needs no dialog: the model closes the sheet. One past the threshold
     * is explained first, then the sheet closes the same way — there is nothing left to refuse.
     */
    fun error(error: JointOperationRejectError): DialogMessage? = when (error) {
        JointOperationRejectError.OperationNotFound -> null
        JointOperationRejectError.ThresholdReached -> DialogMessage(
            title = resourceReference(CoreUiR.string.joint_operation_threshold_reached_title),
            message = resourceReference(CoreUiR.string.joint_operation_threshold_reached_subtitle),
            // The dialog runs onDismissRequest itself once the button is tapped, so the action only carries the title
            onDismissRequest = onOperationGone,
            firstActionBuilder = {
                EventMessageAction(title = resourceReference(CoreUiR.string.common_got_it), onClick = {})
            },
        )
        JointOperationRejectError.NotAMember,
        is JointOperationRejectError.Failed,
        -> DialogMessage(
            title = resourceReference(CoreUiR.string.common_error),
            message = resourceReference(CoreUiR.string.common_something_went_wrong),
        )
    }
}