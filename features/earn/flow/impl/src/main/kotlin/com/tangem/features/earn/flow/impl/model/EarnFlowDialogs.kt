package com.tangem.features.earn.flow.impl.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.modalMessage
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnSessionError
import kotlinx.collections.immutable.persistentListOf

internal object EarnFlowDialogs {

    fun sessionError(error: EarnSessionError): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_error),
        message = error.message(),
    )

    /** For a failure that leaves the screen without data: the user either retries or leaves the flow. */
    fun sessionError(error: EarnSessionError, onRetry: () -> Unit, onClose: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_error),
        message = error.message(),
        firstAction = EventMessageAction(title = resourceReference(R.string.common_retry), onClick = onRetry),
        secondAction = EventMessageAction(title = resourceReference(R.string.common_close), onClick = onClose),
        isDismissable = false,
    )

    fun info(block: EarnTextBlock): ModalMessage = modalMessage {
        content(TangemModalContentUM.Informer.Rich(items = persistentListOf(block.toUM())))
    }

    private fun EarnSessionError.message(): TextReference = when (this) {
        is EarnSessionError.Rejected -> reason
        is EarnSessionError.InsufficientBalance -> resourceReference(R.string.common_insufficient_balance)
        is EarnSessionError.Fee -> resourceReference(R.string.send_fee_unreachable_error_text)
        is EarnSessionError.BelowMinimum,
        is EarnSessionError.ExceedsLimit,
        is EarnSessionError.Failed,
        -> resourceReference(R.string.common_something_went_wrong)
    }
}