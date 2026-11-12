package com.tangem.domain.polymarket.sessionkey.operation

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSigningError

sealed interface PolymarketSessionKeyOperationRequest {

    data object Activate : PolymarketSessionKeyOperationRequest

    data object Reactivate : PolymarketSessionKeyOperationRequest

    data object Extend : PolymarketSessionKeyOperationRequest

    data class Revoke(val sessionKeyAddress: String) : PolymarketSessionKeyOperationRequest

    data class SetEnabled(val sessionKeyAddress: String, val isEnabled: Boolean) : PolymarketSessionKeyOperationRequest
}

enum class PolymarketSessionKeyOperationStage {
    SIGNING,
    REVOKING,
    REVOKED,
    AUTHORIZING,
    CHECKING_READINESS,
    TOGGLING,
}

data class PolymarketSessionKeyOperation(
    val id: Long,
    val request: PolymarketSessionKeyOperationRequest,
    val targetAddress: String?,
    val stage: PolymarketSessionKeyOperationStage,
)

data class PolymarketSessionKeyOperationFailure(
    val error: PolymarketSessionKeyLifecycleError,
    val isAfterRevocation: Boolean,
) {

    val isCancelled: Boolean
        get() = error == PolymarketSessionKeyLifecycleError.Failed(
            PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled),
        )
}

data class PolymarketSessionKeyOperationOutcome(
    val id: Long,
    val request: PolymarketSessionKeyOperationRequest,
    val result: Either<PolymarketSessionKeyOperationFailure, PolymarketSessionKey>,
)

data class PolymarketSessionKeyOperationState(
    val running: PolymarketSessionKeyOperation?,
    val finished: PolymarketSessionKeyOperationOutcome?,
) {

    companion object {
        val IDLE = PolymarketSessionKeyOperationState(running = null, finished = null)
    }
}

sealed interface PolymarketSessionKeyOperationStart {

    data class Started(val id: Long) : PolymarketSessionKeyOperationStart

    data object Busy : PolymarketSessionKeyOperationStart
}