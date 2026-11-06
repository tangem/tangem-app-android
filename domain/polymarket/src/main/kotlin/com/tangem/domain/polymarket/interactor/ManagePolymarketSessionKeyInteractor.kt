package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketDepositWalletLock
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyBatch
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSignedSessionKeyBatches
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyRelay
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.sessionkey.isSettling
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
class ManagePolymarketSessionKeyInteractor(
    private val signBatches: SignPolymarketSessionKeyBatchesInteractor,
    private val sessionKeyRepository: PolymarketSessionKeyRepository,
    polymarketRepository: PolymarketRepository,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val deviceIdProvider: PolymarketDeviceIdProvider,
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val deriveApiCredentials: DeriveApiCredentialsUseCase,
    private val depositWalletLock: PolymarketDepositWalletLock,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val relay = PolymarketSessionKeyRelay(sessionKeyRepository, polymarketRepository, clock)
    private val walletsInOperation: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()

    /**
     * @param replacesSessionKeyAddress the `ACTIVE` key of this device that Polymarket no longer lists
     */
    fun enable(
        userWalletId: UserWalletId,
        replacesSessionKeyAddress: String? = null,
    ): Flow<PolymarketSessionKeyProgress> = operation(userWalletId) {
        val sessionKeyAddress = sessionKeyStore.getOrCreate(userWalletId)
            .mapLeft(PolymarketSessionKeyLifecycleError::Failed)
            .bind()
        enableWith(userWalletId, authorizeBatch(sessionKeyAddress, replacesSessionKeyAddress))
    }

    fun renew(userWalletId: UserWalletId): Flow<PolymarketSessionKeyProgress> = operation(userWalletId) {
        val sessionKeyAddress = sessionKeyStore.heldAddress(userWalletId)
            ?: raise(PolymarketSessionKeyLifecycleError.KeyMissing)
        revokeThenAuthorize(
            userWalletId = userWalletId,
            revokedAddress = sessionKeyAddress,
            authorize = authorizeBatch(sessionKeyAddress = sessionKeyAddress),
        )
    }

    fun replace(
        userWalletId: UserWalletId,
        replacement: PolymarketSessionKeyDeviceState.Replace,
    ): Flow<PolymarketSessionKeyProgress> = operation(userWalletId) {
        val sessionKeyAddress = sessionKeyStore.getOrCreate(userWalletId)
            .mapLeft(PolymarketSessionKeyLifecycleError::Failed)
            .bind()
        val previous = replacement.previous
        val previousValidUntil = previous.validUntil
        when {
            previousValidUntil == null || !clock.instant().isBefore(previousValidUntil) -> {
                enableWith(userWalletId, authorizeBatch(sessionKeyAddress))
            }
            replacement is PolymarketSessionKeyDeviceState.Replace.ByOverwrite -> {
                enableWith(
                    userWalletId = userWalletId,
                    authorize = authorizeBatch(sessionKeyAddress, replacesSessionKeyAddress = previous.address),
                )
            }
            else -> revokeThenAuthorize(
                userWalletId = userWalletId,
                revokedAddress = previous.address,
                authorize = authorizeBatch(sessionKeyAddress),
            )
        }
    }

    fun revoke(userWalletId: UserWalletId, sessionKeyAddress: String): Flow<PolymarketSessionKeyProgress> =
        operation(userWalletId) {
            val revoke = PolymarketSessionKeyBatch.Revoke(sessionKeyAddress = sessionKeyAddress)
            emit(PolymarketSessionKeyProgress.Signing)
            val revoking = depositWalletLock.withLock(userWalletId) {
                val signed = sign(userWalletId, nonEmptyListOf(revoke))
                send { relay.revoke(userWalletId, revocationOf(signed)) }
            }
            awaitRevoked(userWalletId, revoking)
        }

    suspend fun setEnabled(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        isEnabled: Boolean,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> {
        var result = sessionKeyRepository.setSessionKeyEnabled(userWalletId, sessionKeyAddress, isEnabled)
        repeat(BUSY_ATTEMPTS) {
            val error = result.leftOrNull()
            if (error != PolymarketSessionKeyError.SessionKeyBusy && error != PolymarketSessionKeyError.Network) {
                return result
            }
            delay(BUSY_RETRY_DELAY)
            result = sessionKeyRepository.setSessionKeyEnabled(userWalletId, sessionKeyAddress, isEnabled)
        }
        return result
    }

    private suspend fun Operation.enableWith(
        userWalletId: UserWalletId,
        authorize: PolymarketSessionKeyBatch.Authorize,
    ): PolymarketSessionKey {
        emit(PolymarketSessionKeyProgress.Signing)
        val (sent, pending) = depositWalletLock.withLock(userWalletId) {
            sendAuthorization(userWalletId = userWalletId, signed = sign(userWalletId, nonEmptyListOf(authorize)))
        }
        emit(PolymarketSessionKeyProgress.Authorizing(pending))
        return awaitActive(userWalletId, sent.ownerAddress, pending)
    }

    private suspend fun Operation.revokeThenAuthorize(
        userWalletId: UserWalletId,
        revokedAddress: String,
        authorize: PolymarketSessionKeyBatch.Authorize,
    ): PolymarketSessionKey {
        val batches = nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(sessionKeyAddress = revokedAddress), authorize)
        emit(PolymarketSessionKeyProgress.Signing)
        val (signedBoth, revoking) = depositWalletLock.withLock(userWalletId) {
            val signed = sign(userWalletId, batches)
            signed to send { relay.revoke(userWalletId, revocationOf(signed)) }
        }
        emit(PolymarketSessionKeyProgress.Revoked(awaitRevoked(userWalletId, revoking)))
        val (sent, pending) = depositWalletLock.withLock(userWalletId) {
            sendAuthorization(
                userWalletId = userWalletId,
                signed = signedBoth,
                relayerRejectionRetries = FOLLOW_UP_REJECTION_RETRIES,
            )
        }
        emit(PolymarketSessionKeyProgress.Authorizing(pending))
        return awaitActive(userWalletId, sent.ownerAddress, pending)
    }

    /** @param relayerRejectionRetries resends of an authorization the relayer rejected while an action is in flight */
    private suspend fun Operation.sendAuthorization(
        userWalletId: UserWalletId,
        signed: PolymarketSignedSessionKeyBatches,
        relayerRejectionRetries: Int = 0,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        val authorization = authorizationOf(signed)
        var retriesLeft = relayerRejectionRetries
        while (true) {
            ensure(!authorization.isExpired()) {
                TangemLogger.i("Polymarket session key authorization expired before it was sent")
                PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
            }
            val result = relay.authorize(userWalletId, authorization)
            val rejection = result.leftOrNull() as? PolymarketSessionKeyError.RelayerRejected
            if (rejection == null || retriesLeft == 0) return authorization to send { result }
            retriesLeft--
            TangemLogger.i("Polymarket session key authorization rejected by the relayer, sending it again: $rejection")
            delay(RELAYER_REJECTION_RETRY_DELAY)
        }
    }

    private suspend fun Operation.send(
        request: suspend () -> Either<PolymarketSessionKeyError, PolymarketSessionKey>,
    ): PolymarketSessionKey = request()
        .mapLeft { error ->
            if (error.needsNewSignature()) {
                TangemLogger.i("Polymarket session key batch refused for its signature: $error")
                PolymarketSessionKeyLifecycleError.StaleSignature(cause = error)
            } else {
                PolymarketSessionKeyLifecycleError.Failed(error)
            }
        }
        .bind()

    private suspend fun Operation.sign(
        userWalletId: UserWalletId,
        batches: NonEmptyList<PolymarketSessionKeyBatch>,
    ): PolymarketSignedSessionKeyBatches {
        val signed = signBatches(userWalletId, batches).mapLeft(PolymarketSessionKeyLifecycleError::Failed).bind()
        signed.ownerClobAuth?.let { clobAuth ->
            deriveApiCredentials(
                userWalletId = userWalletId,
                ownerAddress = signed.ownerAddress(),
                l1Signature = clobAuth.l1Signature,
                timestamp = clobAuth.timestamp,
            ).onLeft { error -> TangemLogger.e("Polymarket owner credentials not derived with a session key: $error") }
        }
        return signed
    }

    private suspend fun Operation.awaitRevoked(
        userWalletId: UserWalletId,
        revoking: PolymarketSessionKey,
    ): PolymarketSessionKey {
        emit(PolymarketSessionKeyProgress.Revoking(revoking))
        val settled = relay.settle(userWalletId, revoking)
        ensureNotNull(settled) { PolymarketSessionKeyLifecycleError.RevocationFailed(sessionKey = null) }
        ensure(!settled.status.isSettling()) { PolymarketSessionKeyLifecycleError.Unsettled(settled) }
        ensure(settled.status == PolymarketSessionKeyStatus.REVOKED) {
            TangemLogger.e("Polymarket session key ${settled.address} not revoked, back to ${settled.status}")
            PolymarketSessionKeyLifecycleError.RevocationFailed(settled)
        }
        return settled
    }

    private suspend fun Operation.awaitActive(
        userWalletId: UserWalletId,
        ownerAddress: String,
        pending: PolymarketSessionKey,
    ): PolymarketSessionKey {
        val settled = relay.settle(userWalletId, pending) ?: pending
        ensure(!settled.status.isSettling()) { PolymarketSessionKeyLifecycleError.Unsettled(settled) }
        ensure(settled.status == PolymarketSessionKeyStatus.ACTIVE) {
            TangemLogger.e("Polymarket session key ${settled.address} not authorized: ${settled.status}")
            PolymarketSessionKeyLifecycleError.AuthorizationFailed(settled)
        }
        awaitReady(userWalletId = userWalletId, ownerAddress = ownerAddress, active = settled)
        return settled
    }

    private suspend fun Operation.awaitReady(
        userWalletId: UserWalletId,
        ownerAddress: String,
        active: PolymarketSessionKey,
    ) {
        emit(PolymarketSessionKeyProgress.CheckingReadiness(active))
        sessionCredentialsProvider.get(userWalletId)
            .mapLeft { PolymarketSessionKeyLifecycleError.CredentialsUnavailable(sessionKey = active, cause = it) }
            .bind()
        val credentials = getApiCredentials(userWalletId)
            ?: raise(PolymarketSessionKeyLifecycleError.OwnerCredentialsUnavailable(active))
        val validUntil = active.validUntil ?: raise(PolymarketSessionKeyLifecycleError.NotListedOnExchange(active))
        val isListed = relay.awaitListing(
            owner = PolymarketClobIdentity.Owner(address = ownerAddress, credentials = credentials),
            sessionKey = active,
        )
        ensure(isListed) {
            TangemLogger.e("Polymarket does not list the active session key ${active.address}")
            PolymarketSessionKeyLifecycleError.NotListedOnExchange(active)
        }
        tradingStore.markReadyOnExchange(userWalletId, active.address, validUntil)
    }

    private suspend fun authorizeBatch(sessionKeyAddress: String, replacesSessionKeyAddress: String? = null) =
        PolymarketSessionKeyBatch.Authorize(
            sessionKeyAddress = sessionKeyAddress,
            deviceId = deviceIdProvider.deviceId(),
            deviceName = deviceIdProvider.deviceName,
            replacesSessionKeyAddress = replacesSessionKeyAddress,
        )

    private fun operation(
        userWalletId: UserWalletId,
        block: suspend Operation.() -> PolymarketSessionKey,
    ): Flow<PolymarketSessionKeyProgress> = flow {
        val result = exclusively(userWalletId) {
            either { Operation(raise = this, collector = this@flow).block() }
        }
        emit(PolymarketSessionKeyProgress.Finished(result))
    }

    private suspend fun <T> exclusively(
        userWalletId: UserWalletId,
        action: suspend () -> Either<PolymarketSessionKeyLifecycleError, T>,
    ): Either<PolymarketSessionKeyLifecycleError, T> {
        if (!walletsInOperation.add(userWalletId)) {
            TangemLogger.i("Polymarket session key operation refused: another one is in progress for the wallet")
            return PolymarketSessionKeyLifecycleError.OperationInProgress.left()
        }
        return try {
            action()
        } finally {
            walletsInOperation.remove(userWalletId)
        }
    }

    private fun PolymarketSessionKeyAuthorization.isExpired(): Boolean =
        PolymarketRelayerDeadline.isExpired(deadline = deadline, nowMillis = clock.millis() + DEADLINE_MARGIN_MILLIS)

    private fun PolymarketSessionKeyError.needsNewSignature(): Boolean =
        this is PolymarketSessionKeyError.RelayerRejected ||
            this == PolymarketSessionKeyError.NonceAlreadyUsed ||
            this == PolymarketSessionKeyError.InvalidDeadline ||
            this == PolymarketSessionKeyError.InvalidValidUntil

    private fun Raise<PolymarketSessionKeyLifecycleError>.unexpectedBatches(): Nothing =
        raise(PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.MalformedRequest))

    private fun PolymarketSignedSessionKeyBatches.ownerAddress(): String = when (val batch = batches.first()) {
        is PolymarketSignedSessionKeyBatches.Signed.Authorization -> batch.authorization.ownerAddress
        is PolymarketSignedSessionKeyBatches.Signed.Revocation -> batch.revocation.ownerAddress
    }

    private fun Operation.authorizationOf(signed: PolymarketSignedSessionKeyBatches) = signed.batches
        .filterIsInstance<PolymarketSignedSessionKeyBatches.Signed.Authorization>()
        .singleOrNull()
        ?.authorization
        ?: unexpectedBatches()

    private fun Operation.revocationOf(signed: PolymarketSignedSessionKeyBatches) =
        (signed.batches.first() as? PolymarketSignedSessionKeyBatches.Signed.Revocation)?.revocation
            ?: unexpectedBatches()

    private class Operation(
        raise: Raise<PolymarketSessionKeyLifecycleError>,
        collector: FlowCollector<PolymarketSessionKeyProgress>,
    ) : Raise<PolymarketSessionKeyLifecycleError> by raise,
        FlowCollector<PolymarketSessionKeyProgress> by collector

    private companion object {
        const val BUSY_ATTEMPTS = 4
        const val DEADLINE_MARGIN_MILLIS = 30_000L
        const val FOLLOW_UP_REJECTION_RETRIES = 5
        val BUSY_RETRY_DELAY = 1_500.milliseconds
        val RELAYER_REJECTION_RETRY_DELAY = 3.seconds
    }
}