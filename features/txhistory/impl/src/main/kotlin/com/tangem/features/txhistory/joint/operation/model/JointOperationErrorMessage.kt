package com.tangem.features.txhistory.joint.operation.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed.Reason
import com.tangem.utils.converter.Converter
import com.tangem.core.ui.R as CoreUiR

/**
 * Dismissing the card tap shows nothing. A missing owner key is not a security refusal, so it takes the generic
 * text rather than the "contact support" one the other verification reasons share.
 *
 * @property onSupportClick opens the support request
 * @property onOperationGone runs after the user reads that the operation is over — there is nothing left to show
 */
internal class JointOperationErrorMessage(
    private val onSupportClick: () -> Unit,
    private val onOperationGone: () -> Unit,
) : Converter<JointOperationSignError, DialogMessage?> {

    override fun convert(value: JointOperationSignError): DialogMessage? = when (value) {
        JointOperationSignError.UserCancelled -> null
        JointOperationSignError.OperationNotFound -> gone(
            title = CoreUiR.string.joint_operation_completed_title,
            message = CoreUiR.string.joint_operation_completed_subtitle,
        )
        JointOperationSignError.ThresholdReached -> gone(
            title = CoreUiR.string.joint_operation_threshold_reached_title,
            message = CoreUiR.string.joint_operation_threshold_reached_subtitle,
        )
        JointOperationSignError.Malicious -> DialogMessage(
            title = resourceReference(CoreUiR.string.joint_operation_malicious_title),
            message = resourceReference(CoreUiR.string.joint_operation_malicious_subtitle),
            firstActionBuilder = { closeAction() },
        )
        is JointOperationSignError.VerificationFailed -> when (value.reason) {
            Reason.OWNER_KEY_NOT_DERIVED -> generic()
            else -> DialogMessage(
                title = resourceReference(CoreUiR.string.joint_operation_signing_blocked_title),
                message = resourceReference(CoreUiR.string.joint_operation_signing_blocked_subtitle),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreUiR.string.common_support),
                        onClick = onSupportClick,
                    )
                },
                secondActionBuilder = { closeAction() },
            )
        }
        is JointOperationSignError.Failed -> generic()
    }

    private fun gone(title: Int, message: Int) = DialogMessage(
        title = resourceReference(title),
        message = resourceReference(message),
        // The dialog runs onDismissRequest itself once the button is tapped, so the action only carries the title
        onDismissRequest = onOperationGone,
        firstActionBuilder = {
            EventMessageAction(title = resourceReference(CoreUiR.string.common_got_it), onClick = {})
        },
    )

    private fun generic() = DialogMessage(
        title = resourceReference(CoreUiR.string.common_error),
        message = resourceReference(CoreUiR.string.common_something_went_wrong),
    )

    private fun EventMessageAction.BuilderScope.closeAction() = EventMessageAction(
        title = resourceReference(CoreUiR.string.common_close),
        onClick = onDismissRequest,
    )
}