package com.tangem.domain.polymarket.interactor

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorizationStage
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorizationStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyRelay
import com.tangem.domain.polymarket.sessionkey.acceptedIn
import com.tangem.domain.polymarket.sessionkey.stageAt
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock

internal class PolymarketSessionKeyRenewal(
    private val submitter: PolymarketSessionKeySubmitter,
    private val relay: PolymarketSessionKeyRelay,
    private val polymarketRepository: PolymarketRepository,
    private val pendingAuthorizations: PolymarketPendingAuthorizationStore,
    private val clock: Clock,
) {

    suspend fun SessionKeyOperationScope.revokeThenAuthorize(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        ensure(!submitter.isExpired(pending.authorization)) {
            TangemLogger.i("Polymarket session key authorization expired before the revocation was sent")
            PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
        }
        pendingAuthorizations.store(userWalletId, pending)
            .onLeft { TangemLogger.e("Polymarket session key authorization not kept for a resume: $it") }
            .mapLeft(PolymarketSessionKeyLifecycleError::Failed)
            .bind()
        return endingPendingWhenSettled(userWalletId, pending) {
            beforeDeadline(pending.authorization, lastKnown = null) { progress ->
                val revoking = with(submitter) { send { relay.revoke(userWalletId, pending.revocation) } }
                    .also(progress::record)
                authorizeAfterRevocation(
                    userWalletId = userWalletId,
                    interrupted = pending,
                    revoking = revoking,
                    progress = progress,
                )
            }
        }
    }

    suspend fun SessionKeyOperationScope.resume(
        userWalletId: UserWalletId,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        val interrupted = pendingAuthorizations.get(userWalletId)
            .onLeft { TangemLogger.e("Polymarket session key authorization to resume is unreadable: $it") }
            .mapLeft(PolymarketSessionKeyLifecycleError::Failed)
            .bind() ?: run {
            TangemLogger.i("Polymarket session key authorization to resume is gone")
            raise(PolymarketSessionKeyLifecycleError.StaleSignature(cause = null))
        }
        TangemLogger.i("Polymarket session key ${interrupted.purpose} authorization resumed")
        return endingPendingWhenSettled(userWalletId, interrupted) { resumeFromStage(userWalletId, interrupted) }
    }

    suspend fun Raise<PolymarketSessionKeyLifecycleError>.ensureNothingPending(userWalletId: UserWalletId) {
        val pending = pendingAuthorizations.get(userWalletId).getOrElse {
            TangemLogger.e("Polymarket session key operation refused: the waiting authorization is unreadable")
            raise(PolymarketSessionKeyLifecycleError.OperationInProgress)
        } ?: return
        if (pending.isInFlight(clock.millis())) {
            TangemLogger.i("Polymarket session key operation refused: an authorization is waiting to be resumed")
            raise(PolymarketSessionKeyLifecycleError.OperationInProgress)
        }
        pendingAuthorizations.clear(userWalletId)
    }

    private suspend fun SessionKeyOperationScope.resumeFromStage(
        userWalletId: UserWalletId,
        interrupted: PolymarketPendingAuthorization,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        ensure(!submitter.isExpired(interrupted.authorization)) {
            TangemLogger.i("Polymarket session key authorization to resume expired")
            PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
        }
        return beforeDeadline(interrupted.authorization, lastKnown = null) { progress ->
            val walletNonce = polymarketRepository.getRelayerNonce(
                ownerAddress = interrupted.authorization.ownerAddress,
            )
                .onLeft { TangemLogger.e("Polymarket wallet nonce not read to resume an authorization: $it") }
                .mapLeft { PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network) }
                .bind()
            val sessionKeys = polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue)
                .onLeft { TangemLogger.e("Polymarket session keys not read to resume an authorization: $it") }
                .mapLeft { PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network) }
                .bind()
                .sessionKeys
            interrupted.acceptedIn(sessionKeys)?.let { accepted ->
                TangemLogger.i("Polymarket session key authorization to resume was already accepted")
                return@beforeDeadline interrupted.authorization to accepted
            }
            val stage = interrupted.stageAt(sessionKeys, walletNonce, nowMillis = submitter.nowWithMarginMillis())
            TangemLogger.i("Polymarket session key authorization to resume is at ${stage::class.simpleName}")
            val revoking = when (stage) {
                is PolymarketPendingAuthorizationStage.RevocationSent -> stage.revoking.also(progress::record)
                PolymarketPendingAuthorizationStage.RevocationUnsent -> {
                    with(submitter) { send { relay.revoke(userWalletId, interrupted.revocation) } }
                        .also(progress::record)
                }
                PolymarketPendingAuthorizationStage.RevokedRecordDropped -> {
                    val revoked = with(submitter) { ensureUnlisted(userWalletId, interrupted.revocation) }
                        .also(progress::record)
                    emit(PolymarketSessionKeyProgress.Revoked(revoked))
                    return@beforeDeadline sendFollowUpAuthorization(userWalletId, interrupted.authorization)
                }
                is PolymarketPendingAuthorizationStage.RevocationUnconfirmed -> {
                    raise(PolymarketSessionKeyLifecycleError.Unsettled(stage.revoking))
                }
                PolymarketPendingAuthorizationStage.NonceBehind -> {
                    raise(PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network))
                }
                PolymarketPendingAuthorizationStage.Expired,
                PolymarketPendingAuthorizationStage.NonceMoved,
                -> raise(PolymarketSessionKeyLifecycleError.StaleSignature(cause = null))
            }
            authorizeAfterRevocation(
                userWalletId = userWalletId,
                interrupted = interrupted,
                revoking = revoking,
                progress = progress,
            )
        }
    }

    private suspend fun SessionKeyOperationScope.authorizeAfterRevocation(
        userWalletId: UserWalletId,
        interrupted: PolymarketPendingAuthorization,
        revoking: PolymarketSessionKey,
        progress: RenewalProgress,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        val authorization = interrupted.authorization
        val revoked = with(submitter) { awaitRevoked(userWalletId, authorization.ownerAddress, revoking) }
            .also(progress::record)
        emit(PolymarketSessionKeyProgress.Revoked(revoked))
        return sendFollowUpAuthorization(userWalletId, authorization)
    }

    private suspend fun <T : Any> SessionKeyOperationScope.beforeDeadline(
        authorization: PolymarketSessionKeyAuthorization,
        lastKnown: PolymarketSessionKey?,
        block: suspend SessionKeyOperationScope.(RenewalProgress) -> T,
    ): T {
        val progress = RenewalProgress(lastKnown)
        return withTimeoutOrNull(submitter.timeLeft(authorization)) { block(progress) } ?: run {
            delay(PolymarketRelayerDeadline.timeLeft(authorization.deadline, clock.millis()))
            raise(renewalPastDeadline(progress.lastKnown))
        }
    }

    private suspend fun SessionKeyOperationScope.sendFollowUpAuthorization(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> = with(submitter) {
        sendAuthorization(
            userWalletId = userWalletId,
            authorization = authorization,
            relayerRejectionRetries = FOLLOW_UP_REJECTION_RETRIES,
        )
    }

    private suspend fun <T> SessionKeyOperationScope.endingPendingWhenSettled(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
        block: suspend SessionKeyOperationScope.() -> T,
    ): T {
        val result = either {
            SessionKeyOperationScope(raise = this, collector = this@endingPendingWhenSettled).block()
        }
        val isSettled = result.fold(
            ifLeft = { it.provesPairUnusable() && !pending.mayStillLand(clock.millis()) },
            ifRight = { true },
        )
        if (isSettled) {
            pendingAuthorizations.clear(userWalletId)
        }
        return result.bind()
    }

    private companion object {
        const val FOLLOW_UP_REJECTION_RETRIES = 5
    }
}

private class RenewalProgress(var lastKnown: PolymarketSessionKey?) {
    fun record(sessionKey: PolymarketSessionKey) {
        lastKnown = sessionKey
    }
}

private fun renewalPastDeadline(lastKnown: PolymarketSessionKey?): PolymarketSessionKeyLifecycleError {
    TangemLogger.e("Polymarket session key authorization deadline reached during the renewal")
    return if (lastKnown != null) {
        PolymarketSessionKeyLifecycleError.Unsettled(lastKnown)
    } else {
        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
    }
}

private fun PolymarketSessionKeyLifecycleError.provesPairUnusable(): Boolean = when (this) {
    is PolymarketSessionKeyLifecycleError.Failed -> !cause.isInconclusive()
    is PolymarketSessionKeyLifecycleError.StaleSignature -> cause != PolymarketSessionKeyError.NonceAlreadyUsed &&
        cause !is PolymarketSessionKeyError.RelayerRejected
    is PolymarketSessionKeyLifecycleError.Unsettled,
    PolymarketSessionKeyLifecycleError.OperationInProgress,
    -> false
    PolymarketSessionKeyLifecycleError.KeyMissing,
    is PolymarketSessionKeyLifecycleError.AuthorizationFailed,
    is PolymarketSessionKeyLifecycleError.RevocationFailed,
    is PolymarketSessionKeyLifecycleError.CredentialsUnavailable,
    is PolymarketSessionKeyLifecycleError.OwnerCredentialsUnavailable,
    -> true
}

private fun PolymarketSessionKeyError.isInconclusive(): Boolean = this == PolymarketSessionKeyError.Network ||
    this == PolymarketSessionKeyError.UpstreamUnavailable ||
    this == PolymarketSessionKeyError.SessionKeyBusy ||
    this is PolymarketSessionKeyError.Unknown ||
    this is PolymarketSessionKeyError.RelayerRejected ||
    this is PolymarketSessionKeyError.Failure