package com.tangem.features.txhistory.joint.operation.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.core.ui.R as CoreUiR

/**
 * The initiator deletes the operation for everyone, any other member only declines it — two different questions
 * before the same request.
 */
internal class JointOperationRefusalMessages(private val onConfirm: () -> Unit) {

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
     * An operation the backend has already lost needs no dialog: the model closes the sheet. The threshold text is
     * inline until the agreed keys land with the error-texts change.
     */
    fun error(error: JointOperationRejectError): DialogMessage? = when (error) {
        JointOperationRejectError.OperationNotFound -> null
        JointOperationRejectError.ThresholdReached -> DialogMessage(
            title = stringReference("Enough signatures collected"),
            message = stringReference("The operation is being executed. Your signature is not needed"),
            firstActionBuilder = { okAction() },
        )
        JointOperationRejectError.NotAMember,
        is JointOperationRejectError.Failed,
        -> DialogMessage(
            title = resourceReference(CoreUiR.string.common_error),
            message = resourceReference(CoreUiR.string.common_something_went_wrong),
        )
    }
}