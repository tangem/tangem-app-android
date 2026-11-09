package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.isTransient
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class PolymarketSessionKeyRelay(
    private val sessionKeyRepository: PolymarketSessionKeyRepository,
    private val polymarketRepository: PolymarketRepository,
    private val clock: Clock,
) {

    suspend fun authorize(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> = resendUnanswered {
        sessionKeyRepository.authorizeSessionKey(userWalletId, authorization)
    }

    suspend fun revoke(
        userWalletId: UserWalletId,
        revocation: PolymarketSessionKeyRevocation,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> = resendUnanswered {
        sessionKeyRepository.revokeSessionKey(userWalletId, revocation)
    }

    /**
     * @return the key's latest record, still settling if the BFF never settled it, or [Settlement.Dropped] with the
     * last record once the BFF stops listing a key it was revoking
     */
    suspend fun settle(userWalletId: UserWalletId, submitted: PolymarketSessionKey): Settlement {
        var latest = submitted
        var readsWithoutRecord = 0
        repeat(SETTLE_ATTEMPTS) {
            if (!latest.status.isSettling()) return Settlement.Settled(latest)
            delay(POLL_INTERVAL)
            polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue)
                .onLeft { error -> TangemLogger.i("Polymarket session key status not read, still waiting: $error") }
                .onRight { state ->
                    val record = state.sessionKeys.firstOrNull { it.address.equals(submitted.address, true) }
                    when {
                        record != null -> {
                            latest = record
                            readsWithoutRecord = 0
                        }
                        latest.status == PolymarketSessionKeyStatus.REVOKING -> readsWithoutRecord++
                    }
                }
            if (readsWithoutRecord == DROPPED_READS) return Settlement.Dropped(latest)
        }
        return Settlement.Settled(latest)
    }

    /** @return whether Polymarket lists [sessionKey] as able to trade before the attempts run out */
    suspend fun awaitListing(owner: PolymarketClobIdentity.Owner, sessionKey: PolymarketSessionKey): Boolean {
        repeat(LISTING_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(POLL_INTERVAL)
            val signers = sessionKeyRepository.getSessionSigners(owner)
                .onLeft { error -> TangemLogger.i("Polymarket session signers not read, still waiting: $error") }
                .getOrNull()
            if (signers.orEmpty().any { it.lists(sessionKey, now = clock.instant()) }) return true
        }
        return false
    }

    suspend fun awaitUnlisted(owner: PolymarketClobIdentity.Owner, sessionKey: PolymarketSessionKey): Unlisting {
        var unlisting: Unlisting = Unlisting.StillListed
        repeat(LISTING_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(POLL_INTERVAL)
            sessionKeyRepository.getSessionSigners(owner).fold(
                ifLeft = { error ->
                    TangemLogger.d("Polymarket session signers not read while revoking: $error")
                    if (!error.isTransient()) return Unlisting.Unread(error)
                    unlisting = Unlisting.Unread(error)
                },
                ifRight = { signers ->
                    if (signers.none { it.signsFor(sessionKey, now = clock.instant()) }) return Unlisting.Gone
                    unlisting = Unlisting.StillListed
                },
            )
        }
        return unlisting
    }

    private suspend fun resendUnanswered(
        request: suspend () -> Either<PolymarketSessionKeyError, PolymarketSessionKey>,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey> {
        var result = request()
        repeat(RESEND_ATTEMPTS) {
            val error = result.leftOrNull()
            if (error != PolymarketSessionKeyError.Network && error != PolymarketSessionKeyError.UpstreamUnavailable) {
                return result
            }
            TangemLogger.i("Polymarket session key request unanswered ($error), sending the same request again")
            delay(POLL_INTERVAL)
            result = request()
        }
        return result
    }

    sealed interface Settlement {
        data class Settled(val sessionKey: PolymarketSessionKey) : Settlement

        data class Dropped(val sessionKey: PolymarketSessionKey) : Settlement
    }

    sealed interface Unlisting {
        data object Gone : Unlisting

        data object StillListed : Unlisting

        data class Unread(val error: PolymarketAuthError) : Unlisting
    }

    companion object {
        val POLL_INTERVAL: Duration = 3.seconds
        private val SETTLE_TIMEOUT: Duration = 35.minutes
        private val SETTLE_ATTEMPTS = (SETTLE_TIMEOUT / POLL_INTERVAL).toInt()
        private const val DROPPED_READS = 2
        private const val LISTING_ATTEMPTS = 20
        private const val RESEND_ATTEMPTS = 3
    }
}

internal fun PolymarketSessionKeyStatus.isSettling(): Boolean =
    this == PolymarketSessionKeyStatus.PENDING || this == PolymarketSessionKeyStatus.REVOKING

internal fun PolymarketSessionSigner.lists(sessionKey: PolymarketSessionKey, now: Instant): Boolean =
    signsFor(sessionKey, now = now.plusMillis(LISTING_EXPIRY_MARGIN.inWholeMilliseconds)) &&
        scopes.any { it.uppercase() in TRADING_SCOPES }

private fun PolymarketSessionSigner.signsFor(sessionKey: PolymarketSessionKey, now: Instant): Boolean {
    val validUntil = validUntil ?: return false
    return address.equals(sessionKey.address, ignoreCase = true) && validUntil > now
}

private val TRADING_SCOPES = setOf("CLOB", "ALL")
private val LISTING_EXPIRY_MARGIN: Duration = 10.minutes