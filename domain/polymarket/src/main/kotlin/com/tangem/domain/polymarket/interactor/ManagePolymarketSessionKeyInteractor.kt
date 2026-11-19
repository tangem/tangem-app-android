package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketDepositWalletLock
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyBatch
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSignedSessionKeyBatches
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorizationStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyRelay
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LongParameterList")
class ManagePolymarketSessionKeyInteractor(
    private val signBatches: SignPolymarketSessionKeyBatchesInteractor,
    private val sessionKeyRepository: PolymarketSessionKeyRepository,
    private val polymarketRepository: PolymarketRepository,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val deviceIdProvider: PolymarketDeviceIdProvider,
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val deriveApiCredentials: DeriveApiCredentialsUseCase,
    private val depositWalletLock: PolymarketDepositWalletLock,
    private val pendingAuthorizations: PolymarketPendingAuthorizationStore,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val relay = PolymarketSessionKeyRelay(sessionKeyRepository, polymarketRepository, clock)
    private val submitter = PolymarketSessionKeySubmitter(
        relay = relay,
        sessionCredentialsProvider = sessionCredentialsProvider,
        tradingStore = tradingStore,
        getApiCredentials = getApiCredentials,
        clock = clock,
    )
    private val renewal = PolymarketSessionKeyRenewal(
        submitter = submitter,
        relay = relay,
        polymarketRepository = polymarketRepository,
        pendingAuthorizations = pendingAuthorizations,
        clock = clock,
    )
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
            purpose = PolymarketPendingAuthorization.Purpose.EXTEND,
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
                purpose = PolymarketPendingAuthorization.Purpose.REPLACE,
            )
        }
    }

    fun revoke(userWalletId: UserWalletId, sessionKeyAddress: String): Flow<PolymarketSessionKeyProgress> =
        operation(userWalletId) {
            val revoke = PolymarketSessionKeyBatch.Revoke(sessionKeyAddress = sessionKeyAddress)
            emit(PolymarketSessionKeyProgress.Signing)
            val (signed, revoking) = depositWalletLock.withSessionKeyOperation(userWalletId) {
                val signed = sign(userWalletId, nonEmptyListOf(revoke))
                signed to with(submitter) { send { relay.revoke(userWalletId, revocationOf(signed)) } }
            }
            with(submitter) { awaitRevoked(userWalletId, signed.ownerAddress(), revoking) }
        }

    fun resume(userWalletId: UserWalletId): Flow<PolymarketSessionKeyProgress> =
        operation(userWalletId, isResume = true) {
            val (sent, pending) = depositWalletLock.withSessionKeyOperation(userWalletId) {
                with(renewal) { resume(userWalletId) }
            }
            emit(PolymarketSessionKeyProgress.Authorizing(pending))
            with(submitter) { awaitActive(userWalletId, sent.ownerAddress, pending) }
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

    private suspend fun SessionKeyOperationScope.enableWith(
        userWalletId: UserWalletId,
        authorize: PolymarketSessionKeyBatch.Authorize,
    ): PolymarketSessionKey {
        emit(PolymarketSessionKeyProgress.Signing)
        val (sent, pending) = depositWalletLock.withSessionKeyOperation(userWalletId) {
            val signed = sign(userWalletId, nonEmptyListOf(authorize))
            with(submitter) { sendAuthorization(userWalletId = userWalletId, authorization = authorizationOf(signed)) }
        }
        emit(PolymarketSessionKeyProgress.Authorizing(pending))
        return with(submitter) { awaitActive(userWalletId, sent.ownerAddress, pending) }
    }

    private suspend fun SessionKeyOperationScope.revokeThenAuthorize(
        userWalletId: UserWalletId,
        revokedAddress: String,
        authorize: PolymarketSessionKeyBatch.Authorize,
        purpose: PolymarketPendingAuthorization.Purpose,
    ): PolymarketSessionKey {
        val batches = nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(sessionKeyAddress = revokedAddress), authorize)
        emit(PolymarketSessionKeyProgress.Signing)
        val (sent, pending) = depositWalletLock.withSessionKeyOperation(userWalletId) {
            val signed = sign(userWalletId, batches)
            val interrupted = PolymarketPendingAuthorization(
                revocation = revocationOf(signed),
                authorization = authorizationOf(signed),
                purpose = purpose,
            )
            with(renewal) { revokeThenAuthorize(userWalletId, interrupted) }
        }
        emit(PolymarketSessionKeyProgress.Authorizing(pending))
        return with(submitter) { awaitActive(userWalletId, sent.ownerAddress, pending) }
    }

    private suspend fun SessionKeyOperationScope.sign(
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

    private suspend fun authorizeBatch(sessionKeyAddress: String, replacesSessionKeyAddress: String? = null) =
        PolymarketSessionKeyBatch.Authorize(
            sessionKeyAddress = sessionKeyAddress,
            deviceId = deviceIdProvider.deviceId(),
            deviceName = deviceIdProvider.deviceName,
            replacesSessionKeyAddress = replacesSessionKeyAddress,
        )

    private fun operation(
        userWalletId: UserWalletId,
        isResume: Boolean = false,
        block: suspend SessionKeyOperationScope.() -> PolymarketSessionKey,
    ): Flow<PolymarketSessionKeyProgress> = flow {
        val result = exclusively(userWalletId) {
            either {
                if (!isResume) with(renewal) { ensureNothingPending(userWalletId) }
                SessionKeyOperationScope(raise = this, collector = this@flow).block()
            }
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

    private fun Raise<PolymarketSessionKeyLifecycleError>.unexpectedBatches(): Nothing =
        raise(PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.MalformedRequest))

    private fun SessionKeyOperationScope.authorizationOf(signed: PolymarketSignedSessionKeyBatches) = signed.batches
        .filterIsInstance<PolymarketSignedSessionKeyBatches.Signed.Authorization>()
        .singleOrNull()
        ?.authorization
        ?: unexpectedBatches()

    private fun SessionKeyOperationScope.revocationOf(signed: PolymarketSignedSessionKeyBatches) =
        (signed.batches.first() as? PolymarketSignedSessionKeyBatches.Signed.Revocation)?.revocation
            ?: unexpectedBatches()

    private companion object {
        const val BUSY_ATTEMPTS = 4
        val BUSY_RETRY_DELAY = 1_500.milliseconds
    }
}

private fun PolymarketSignedSessionKeyBatches.ownerAddress(): String = when (val batch = batches.first()) {
    is PolymarketSignedSessionKeyBatches.Signed.Authorization -> batch.authorization.ownerAddress
    is PolymarketSignedSessionKeyBatches.Signed.Revocation -> batch.revocation.ownerAddress
}