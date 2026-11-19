package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyRelay
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.sessionkey.isSettling
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class PolymarketSessionKeySubmitter(
    private val relay: PolymarketSessionKeyRelay,
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val clock: Clock,
) {

    /** @param relayerRejectionRetries resends of an authorization the relayer rejected while an action is in flight */
    suspend fun SessionKeyOperationScope.sendAuthorization(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
        relayerRejectionRetries: Int = 0,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        var retriesLeft = relayerRejectionRetries
        while (true) {
            ensure(!isExpired(authorization)) {
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

    suspend fun SessionKeyOperationScope.send(
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

    suspend fun SessionKeyOperationScope.awaitRevoked(
        userWalletId: UserWalletId,
        ownerAddress: String,
        revoking: PolymarketSessionKey,
    ): PolymarketSessionKey {
        emit(PolymarketSessionKeyProgress.Revoking(revoking))
        val settled = when (val settlement = relay.settle(userWalletId, revoking)) {
            is PolymarketSessionKeyRelay.Settlement.Settled -> settlement.sessionKey
            is PolymarketSessionKeyRelay.Settlement.Dropped -> {
                confirmUnlisted(userWalletId, ownerAddress, dropped = settlement.sessionKey)
            }
        }
        ensure(!settled.status.isSettling()) { PolymarketSessionKeyLifecycleError.Unsettled(settled) }
        ensure(settled.status == PolymarketSessionKeyStatus.REVOKED) {
            TangemLogger.e("Polymarket session key ${settled.address} not revoked, back to ${settled.status}")
            PolymarketSessionKeyLifecycleError.RevocationFailed(settled)
        }
        return settled
    }

    suspend fun SessionKeyOperationScope.awaitActive(
        userWalletId: UserWalletId,
        ownerAddress: String,
        pending: PolymarketSessionKey,
    ): PolymarketSessionKey {
        val settled = when (val settlement = relay.settle(userWalletId, pending)) {
            is PolymarketSessionKeyRelay.Settlement.Settled -> settlement.sessionKey
            is PolymarketSessionKeyRelay.Settlement.Dropped -> {
                TangemLogger.e("Polymarket session key ${settlement.sessionKey.address} dropped while authorizing")
                raise(PolymarketSessionKeyLifecycleError.AuthorizationFailed(settlement.sessionKey))
            }
        }
        ensure(!settled.status.isSettling()) { PolymarketSessionKeyLifecycleError.Unsettled(settled) }
        ensure(settled.status == PolymarketSessionKeyStatus.ACTIVE) {
            TangemLogger.e("Polymarket session key ${settled.address} not authorized: ${settled.status}")
            PolymarketSessionKeyLifecycleError.AuthorizationFailed(settled)
        }
        tradingStore.markAuthorized(userWalletId, settled.address)
        awaitReady(userWalletId = userWalletId, ownerAddress = ownerAddress, active = settled)
        return settled
    }

    fun isExpired(authorization: PolymarketSessionKeyAuthorization): Boolean = !timeLeft(authorization).isPositive()

    fun timeLeft(authorization: PolymarketSessionKeyAuthorization): Duration = PolymarketRelayerDeadline.timeLeft(
        deadline = authorization.deadline,
        nowMillis = nowWithMarginMillis(),
    )

    fun nowWithMarginMillis(): Long = clock.millis() + PolymarketRelayerDeadline.SEND_MARGIN_MILLIS

    private suspend fun SessionKeyOperationScope.confirmUnlisted(
        userWalletId: UserWalletId,
        ownerAddress: String,
        dropped: PolymarketSessionKey,
    ): PolymarketSessionKey {
        val credentials = getApiCredentials(userWalletId) ?: run {
            TangemLogger.e("Polymarket session key ${dropped.address} dropped while revoking, no owner to check it")
            raise(PolymarketSessionKeyLifecycleError.Unsettled(dropped))
        }
        val owner = PolymarketClobIdentity.Owner(address = ownerAddress, credentials = credentials)
        val unlisting = relay.awaitUnlisted(owner, dropped)
        ensure(unlisting == PolymarketSessionKeyRelay.Unlisting.Gone) {
            TangemLogger.e("Polymarket session key ${dropped.address} dropped while revoking, unconfirmed: $unlisting")
            PolymarketSessionKeyLifecycleError.Unsettled(dropped)
        }
        TangemLogger.i("Polymarket session key ${dropped.address} dropped while revoking and unlisted, revoked")
        return dropped.copy(status = PolymarketSessionKeyStatus.REVOKED)
    }

    private suspend fun SessionKeyOperationScope.awaitReady(
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
        val validUntil = active.validUntil
        val isListed = validUntil != null && relay.awaitListing(
            owner = PolymarketClobIdentity.Owner(address = ownerAddress, credentials = credentials),
            sessionKey = active,
        )
        if (validUntil == null || !isListed) {
            TangemLogger.i("Polymarket does not list the active session key ${active.address} yet, left to reconcile")
            return
        }
        tradingStore.markReadyOnExchange(userWalletId, active.address, validUntil)
    }

    private companion object {
        val RELAYER_REJECTION_RETRY_DELAY = 3.seconds
    }
}

private fun PolymarketSessionKeyError.needsNewSignature(): Boolean =
    this is PolymarketSessionKeyError.RelayerRejected ||
        this == PolymarketSessionKeyError.NonceAlreadyUsed ||
        this == PolymarketSessionKeyError.InvalidDeadline ||
        this == PolymarketSessionKeyError.InvalidValidUntil