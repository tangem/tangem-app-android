package com.tangem.features.polymarket.impl.fasttrading.model

import androidx.annotation.StringRes
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.core.ui.R as CoreUiR

internal object PolymarketSessionKeyMessagesFactory {

    private val BUSY_ERRORS = setOf(
        PolymarketSessionKeyLifecycleError.OperationInProgress,
        PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy),
    )

    fun create(outcome: PolymarketSessionKeyOperationOutcome): SnackbarMessage? = outcome.result.fold(
        ifLeft = { failure -> failureToast(outcome.request, failure) },
        ifRight = { successToast(outcome.request) },
    )

    fun create(start: PolymarketSessionKeyOperationStart): SnackbarMessage? = when (start) {
        PolymarketSessionKeyOperationStart.Busy -> errorToast(R.string.prediction_session_key_toast_busy)
        is PolymarketSessionKeyOperationStart.Started -> null
    }

    private fun successToast(request: PolymarketSessionKeyOperationRequest): SnackbarMessage? = when (request) {
        is PolymarketSessionKeyOperationRequest.Revoke -> SnackbarMessage(
            message = resourceReference(R.string.prediction_session_key_toast_revoked),
            startIconId = CoreUiR.drawable.ic_success_20,
            isIconTinted = false,
        )
        PolymarketSessionKeyOperationRequest.Activate,
        PolymarketSessionKeyOperationRequest.Reactivate,
        PolymarketSessionKeyOperationRequest.Extend,
        is PolymarketSessionKeyOperationRequest.SetEnabled,
        -> null
    }

    private fun failureToast(
        request: PolymarketSessionKeyOperationRequest,
        failure: PolymarketSessionKeyOperationFailure,
    ): SnackbarMessage? = when {
        failure.isCancelled -> null
        failure.isBusy() -> errorToast(R.string.prediction_session_key_toast_busy)
        else -> errorToast(failureMessageId(request, failure.isAfterRevocation))
    }

    @StringRes
    private fun failureMessageId(request: PolymarketSessionKeyOperationRequest, isAfterRevocation: Boolean): Int =
        when (request) {
            PolymarketSessionKeyOperationRequest.Activate -> R.string.prediction_session_key_toast_activation_failed
            PolymarketSessionKeyOperationRequest.Reactivate -> if (isAfterRevocation) {
                R.string.prediction_session_key_toast_reactivation_failed_revoked
            } else {
                R.string.prediction_session_key_toast_reactivation_failed
            }
            PolymarketSessionKeyOperationRequest.Extend -> if (isAfterRevocation) {
                R.string.prediction_session_key_toast_extension_failed_revoked
            } else {
                R.string.prediction_session_key_toast_extension_failed
            }
            is PolymarketSessionKeyOperationRequest.Revoke -> R.string.prediction_session_key_toast_revocation_failed
            is PolymarketSessionKeyOperationRequest.SetEnabled -> if (request.isEnabled) {
                R.string.prediction_session_key_toast_resuming_failed
            } else {
                R.string.prediction_session_key_toast_pausing_failed
            }
        }

    private fun PolymarketSessionKeyOperationFailure.isBusy(): Boolean = !isAfterRevocation && error in BUSY_ERRORS

    private fun errorToast(@StringRes messageId: Int): SnackbarMessage = SnackbarMessage(
        message = resourceReference(messageId),
        startIconId = CoreUiR.drawable.ic_alert_circle_24,
    )
}