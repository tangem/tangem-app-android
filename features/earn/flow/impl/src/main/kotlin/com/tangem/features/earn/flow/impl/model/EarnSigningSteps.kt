package com.tangem.features.earn.flow.impl.model

import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * Calls [onUnfinished] when the signing stream throws or ends without [EarnSigningStep.Done] or
 * [EarnSigningStep.Failed], with the transaction that was already sent, if any. Cancellation is not reported.
 */
internal fun Flow<EarnSigningStep>.onUnfinished(
    onUnfinished: (sentRef: EarnTxRef?, error: Throwable?) -> Unit,
): Flow<EarnSigningStep> {
    var sentRef: EarnTxRef? = null
    var isFinished = false
    return onEach { step ->
        when (step) {
            is EarnSigningStep.Sent -> sentRef = step.ref
            is EarnSigningStep.Done, is EarnSigningStep.Failed -> isFinished = true
            is EarnSigningStep.AwaitingConfirmation -> Unit
        }
    }
        .catch { error ->
            isFinished = true
            onUnfinished(sentRef, error)
        }
        .onCompletion { cause ->
            if (!isFinished && cause !is CancellationException) onUnfinished(sentRef, null)
        }
}