package com.tangem.domain.polymarket.sessionkey.operation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.ManagePolymarketSessionKeyInteractor
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private typealias OperationResult = Either<PolymarketSessionKeyOperationFailure, PolymarketSessionKey>

class PolymarketSessionKeyOperationRunner(
    private val manage: ManagePolymarketSessionKeyInteractor,
    private val stateService: PolymarketSessionKeyStateService,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val appScope: AppCoroutineScope,
) {

    private val states = ConcurrentHashMap<UserWalletId, MutableStateFlow<PolymarketSessionKeyOperationState>>()
    private val lastId = AtomicLong()

    fun observe(userWalletId: UserWalletId): StateFlow<PolymarketSessionKeyOperationState> =
        stateOf(userWalletId).asStateFlow()

    @Synchronized
    fun start(
        userWalletId: UserWalletId,
        request: PolymarketSessionKeyOperationRequest,
    ): PolymarketSessionKeyOperationStart {
        val state = stateOf(userWalletId)
        if (state.value.running != null) {
            TangemLogger.i("Polymarket session key ${request.kind} refused: another operation is running")
            return PolymarketSessionKeyOperationStart.Busy
        }
        val operation = PolymarketSessionKeyOperation(
            id = lastId.incrementAndGet(),
            request = request,
            targetAddress = request.addressOrNull(),
            stage = request.initialStage(),
        )
        state.value = PolymarketSessionKeyOperationState(running = operation, finished = null)
        TangemLogger.i("Polymarket session key ${request.kind} started")
        appScope.launch { run(userWalletId, operation, state) }
        return PolymarketSessionKeyOperationStart.Started(operation.id)
    }

    fun acknowledge(userWalletId: UserWalletId, operationId: Long) {
        states[userWalletId]?.update { state ->
            if (state.finished?.id == operationId) state.copy(finished = null) else state
        }
    }

    private suspend fun run(
        userWalletId: UserWalletId,
        operation: PolymarketSessionKeyOperation,
        state: MutableStateFlow<PolymarketSessionKeyOperationState>,
    ) {
        val tracker = Tracker(userWalletId, operation.id, state)
        try {
            val result = runSuspendCatching { execute(userWalletId, operation.request, tracker) }
                .getOrElse { cause ->
                    TangemLogger.e("Polymarket session key ${operation.request.kind} threw", cause)
                    PolymarketSessionKeyOperationFailure(
                        error = PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Failure(cause)),
                        isAfterRevocation = tracker.isRevoked,
                    ).left()
                }
            log(operation.request, result)
            stateService.refreshAndAwait(userWalletId)
            state.value = PolymarketSessionKeyOperationState(
                running = null,
                finished = PolymarketSessionKeyOperationOutcome(operation.id, operation.request, result),
            )
        } finally {
            state.update { current ->
                if (current.running?.id == operation.id) current.copy(running = null) else current
            }
        }
    }

    private suspend fun execute(
        userWalletId: UserWalletId,
        request: PolymarketSessionKeyOperationRequest,
        tracker: Tracker,
    ): OperationResult = when (request) {
        PolymarketSessionKeyOperationRequest.Activate -> activate(userWalletId, tracker)
        PolymarketSessionKeyOperationRequest.Reactivate -> {
            sessionKeyStore.heldAddress(userWalletId)?.let(tracker::target)
            tracker.follow(manage.enable(userWalletId))
        }
        PolymarketSessionKeyOperationRequest.Extend -> {
            sessionKeyStore.heldAddress(userWalletId)?.let(tracker::target)
            tracker.follow(manage.renew(userWalletId))
        }
        is PolymarketSessionKeyOperationRequest.Revoke -> {
            tracker.follow(manage.revoke(userWalletId, request.sessionKeyAddress))
        }
        is PolymarketSessionKeyOperationRequest.SetEnabled -> {
            manage.setEnabled(userWalletId, request.sessionKeyAddress, request.isEnabled).mapLeft { error ->
                PolymarketSessionKeyOperationFailure(
                    error = PolymarketSessionKeyLifecycleError.Failed(error),
                    isAfterRevocation = false,
                )
            }
        }
    }

    private suspend fun activate(userWalletId: UserWalletId, tracker: Tracker): OperationResult {
        val overview = stateService.observe(userWalletId).first().getOrNull()
        val deviceState = overview?.assessment?.deviceState
        (deviceState?.ownAddress() ?: sessionKeyStore.heldAddress(userWalletId))?.let(tracker::target)
        return when (deviceState) {
            null,
            PolymarketSessionKeyDeviceState.OfferEnable,
            is PolymarketSessionKeyDeviceState.Reenable,
            -> tracker.follow(manage.enable(userWalletId))
            is PolymarketSessionKeyDeviceState.RenewalRequired,
            is PolymarketSessionKeyDeviceState.RepairByRenewal,
            -> tracker.follow(manage.renew(userWalletId))
            is PolymarketSessionKeyDeviceState.Replace -> tracker.follow(manage.replace(userWalletId, deviceState))
            is PolymarketSessionKeyDeviceState.Active -> deviceState.sessionKey.right()
            is PolymarketSessionKeyDeviceState.Disabled -> deviceState.sessionKey.right()
            is PolymarketSessionKeyDeviceState.Enabling,
            is PolymarketSessionKeyDeviceState.Revoking,
            is PolymarketSessionKeyDeviceState.WaitingForPrevious,
            -> PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.OperationInProgress,
                isAfterRevocation = false,
            ).left()
        }
    }

    private fun log(request: PolymarketSessionKeyOperationRequest, result: OperationResult) {
        result.fold(
            ifLeft = { failure ->
                if (failure.isCancelled) {
                    TangemLogger.i("Polymarket session key ${request.kind} cancelled at signing")
                } else {
                    TangemLogger.e(
                        "Polymarket session key ${request.kind} failed " +
                            "(after revocation: ${failure.isAfterRevocation}): ${failure.error}",
                    )
                }
            },
            ifRight = { sessionKey ->
                TangemLogger.i("Polymarket session key ${request.kind} finished: ${sessionKey.address}")
            },
        )
    }

    private fun stateOf(userWalletId: UserWalletId): MutableStateFlow<PolymarketSessionKeyOperationState> =
        states.computeIfAbsent(userWalletId) { MutableStateFlow(PolymarketSessionKeyOperationState.IDLE) }

    private inner class Tracker(
        private val userWalletId: UserWalletId,
        private val operationId: Long,
        private val state: MutableStateFlow<PolymarketSessionKeyOperationState>,
    ) {

        var isRevoked = false
            private set

        fun target(address: String) {
            advance(stage = null, targetAddress = address)
        }

        suspend fun follow(progress: Flow<PolymarketSessionKeyProgress>): OperationResult {
            val finished = progress
                .onEach(::report)
                .filterIsInstance<PolymarketSessionKeyProgress.Finished>()
                .first()
            return finished.result.mapLeft { error ->
                PolymarketSessionKeyOperationFailure(error = error, isAfterRevocation = isRevoked)
            }
        }

        private fun report(step: PolymarketSessionKeyProgress) {
            when (step) {
                PolymarketSessionKeyProgress.Signing -> advance(PolymarketSessionKeyOperationStage.SIGNING)
                is PolymarketSessionKeyProgress.Revoking -> {
                    advance(PolymarketSessionKeyOperationStage.REVOKING, step.sessionKey.address)
                    stateService.refresh(userWalletId)
                }
                is PolymarketSessionKeyProgress.Revoked -> {
                    isRevoked = true
                    advance(PolymarketSessionKeyOperationStage.REVOKED)
                }
                is PolymarketSessionKeyProgress.Authorizing -> {
                    advance(PolymarketSessionKeyOperationStage.AUTHORIZING, step.sessionKey.address)
                    stateService.refresh(userWalletId)
                }
                is PolymarketSessionKeyProgress.CheckingReadiness -> {
                    advance(PolymarketSessionKeyOperationStage.CHECKING_READINESS, step.sessionKey.address)
                }
                is PolymarketSessionKeyProgress.Finished -> Unit
            }
        }

        private fun advance(stage: PolymarketSessionKeyOperationStage?, targetAddress: String? = null) {
            state.update { current ->
                val running = current.running?.takeIf { it.id == operationId } ?: return@update current
                current.copy(
                    running = running.copy(
                        stage = stage ?: running.stage,
                        targetAddress = targetAddress ?: running.targetAddress,
                    ),
                )
            }
        }
    }
}

private val PolymarketSessionKeyOperationRequest.kind: String
    get() = when (this) {
        PolymarketSessionKeyOperationRequest.Activate -> "activation"
        PolymarketSessionKeyOperationRequest.Reactivate -> "reactivation"
        PolymarketSessionKeyOperationRequest.Extend -> "extension"
        is PolymarketSessionKeyOperationRequest.Revoke -> "revocation of $sessionKeyAddress"
        is PolymarketSessionKeyOperationRequest.SetEnabled -> "toggle of $sessionKeyAddress to $isEnabled"
    }

private fun PolymarketSessionKeyOperationRequest.addressOrNull(): String? = when (this) {
    is PolymarketSessionKeyOperationRequest.Revoke -> sessionKeyAddress
    is PolymarketSessionKeyOperationRequest.SetEnabled -> sessionKeyAddress
    PolymarketSessionKeyOperationRequest.Activate,
    PolymarketSessionKeyOperationRequest.Reactivate,
    PolymarketSessionKeyOperationRequest.Extend,
    -> null
}

private fun PolymarketSessionKeyOperationRequest.initialStage(): PolymarketSessionKeyOperationStage =
    if (this is PolymarketSessionKeyOperationRequest.SetEnabled) {
        PolymarketSessionKeyOperationStage.TOGGLING
    } else {
        PolymarketSessionKeyOperationStage.SIGNING
    }

private fun PolymarketSessionKeyDeviceState.ownAddress(): String? = when (this) {
    PolymarketSessionKeyDeviceState.OfferEnable -> null
    is PolymarketSessionKeyDeviceState.Enabling -> sessionKey.address
    is PolymarketSessionKeyDeviceState.Revoking -> sessionKey.address
    is PolymarketSessionKeyDeviceState.Active -> sessionKey.address
    is PolymarketSessionKeyDeviceState.Disabled -> sessionKey.address
    is PolymarketSessionKeyDeviceState.RenewalRequired -> sessionKey.address
    is PolymarketSessionKeyDeviceState.Reenable -> sessionKey.address
    is PolymarketSessionKeyDeviceState.RepairByRenewal -> sessionKey.address
    is PolymarketSessionKeyDeviceState.WaitingForPrevious -> null
    is PolymarketSessionKeyDeviceState.Replace -> null
}