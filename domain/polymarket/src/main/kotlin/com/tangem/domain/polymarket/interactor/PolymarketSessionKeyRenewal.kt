package com.tangem.domain.polymarket.interactor

import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyRelay
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock

internal class PolymarketSessionKeyRenewal(
    private val submitter: PolymarketSessionKeySubmitter,
    private val relay: PolymarketSessionKeyRelay,
    private val clock: Clock,
) {

    suspend fun SessionKeyOperationScope.revokeThenAuthorize(
        userWalletId: UserWalletId,
        revocation: PolymarketSessionKeyRevocation,
        authorization: PolymarketSessionKeyAuthorization,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
        ensure(!submitter.isExpired(authorization)) {
            TangemLogger.i("Polymarket session key authorization expired before the revocation was sent")
            PolymarketSessionKeyLifecycleError.StaleSignature(cause = null)
        }
        return beforeDeadline(authorization, lastKnown = null) { progress ->
            val revoking = with(submitter) { send { relay.revoke(userWalletId, revocation) } }
                .also(progress::record)
            authorizeAfterRevocation(
                userWalletId = userWalletId,
                authorization = authorization,
                revoking = revoking,
                progress = progress,
            )
        }
    }

    private suspend fun SessionKeyOperationScope.authorizeAfterRevocation(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
        revoking: PolymarketSessionKey,
        progress: RenewalProgress,
    ): Pair<PolymarketSessionKeyAuthorization, PolymarketSessionKey> {
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