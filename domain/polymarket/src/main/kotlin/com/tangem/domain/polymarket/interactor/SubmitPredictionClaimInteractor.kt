package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.domain.polymarket.redeem.PredictionRedeemCallValidator
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

sealed interface PredictionClaimFailure {

    data object CallUnavailable : PredictionClaimFailure

    /** The backend has nothing left to redeem for the market — claimed elsewhere already, most likely. */
    data class NothingToRedeem(val detail: String?) : PredictionClaimFailure

    data object NonceUnavailable : PredictionClaimFailure

    /** The position's market version could not be learned, so the redeem calls cannot be checked. Retryable. */
    data class VersionUnavailable(val detail: String?) : PredictionClaimFailure

    /** The redeem calls the backend served do not redeem this position. Nothing was signed. */
    data class CallRejected(val rejection: PredictionRedeemCallValidator.Rejection) : PredictionClaimFailure

    data class SigningFailed(val error: PolymarketSigningError) : PredictionClaimFailure

    data object Expired : PredictionClaimFailure

    data class Rejected(val error: PolymarketRedeemError) : PredictionClaimFailure

    /** The claim was rejected on chain, or the backend stopped following it. Nothing left the account. */
    data class Failed(val receipt: PredictionClaimReceipt) : PredictionClaimFailure

    /**
     * The claim was accepted but did not settle within the screen's waiting window, or the backend stopped
     * answering for it. It may still land — refresh the positions before telling the user anything.
     */
    data class Unconfirmed(val receipt: PredictionClaimReceipt) : PredictionClaimFailure
}

class SubmitPredictionClaimInteractor(
    private val redeemRepository: PolymarketRedeemRepository,
    private val polymarketRepository: PolymarketRepository,
    private val positionsRepository: PolymarketPositionsRepository,
    private val signer: PolymarketTypedDataSigner,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    /**
     * @return the settled receipt of the claim; the backend keys a claim on (wallet, market), so a resumed
     *  screen or a repeated tap converges on the claim already in flight rather than relaying a second one
     */
    suspend fun claim(params: Params): Either<PredictionClaimFailure, PredictionClaimReceipt> = either {
        settle(params, submit(params))
    }

    private suspend fun Raise<PredictionClaimFailure>.submit(params: Params): PredictionClaimReceipt {
        val served = redeemRepository.getCalls(
            walletId = params.walletId,
            conditionId = params.conditionId,
            isNegRisk = params.isNegRisk,
        ).getOrElse { error ->
            if (error is PolymarketRedeemError.NothingToRedeem) {
                raise(PredictionClaimFailure.NothingToRedeem(detail = error.detail))
            }
            raise(PredictionClaimFailure.CallUnavailable)
        }
        val version = resolveVersion(params = params, served = served.version)
        PredictionRedeemCallValidator.validate(
            calls = served,
            expectation = PredictionRedeemCallValidator.Expectation(
                version = version,
                conditionId = params.conditionId,
                assetId = params.assetId,
                isNegRisk = params.isNegRisk,
            ),
        ).getOrElse { raise(PredictionClaimFailure.CallRejected(it)) }
        val calls = served.calls

        val nonce = polymarketRepository.getRelayerNonce(params.ownerAddress)
            .getOrElse { raise(PredictionClaimFailure.NonceUnavailable) }
            .toString()

        val deadline = PolymarketRelayerDeadline.fromNow(nowMillis = currentTimeMillis())

        val signature = signer.signRelayedBatch(
            userWalletId = params.userWalletId,
            payload = PolymarketApprovalsPayload(
                depositWalletAddress = params.depositWalletAddress,
                nonce = nonce,
                deadline = deadline,
                calls = calls,
            ),
        ).getOrElse { raise(PredictionClaimFailure.SigningFailed(it)) }
        ensure(!PolymarketRelayerDeadline.isExpired(deadline, nowMillis = currentTimeMillis())) {
            PredictionClaimFailure.Expired
        }

        val batch = PredictionClaimBatch(
            ownerAddress = params.ownerAddress,
            depositWalletAddress = params.depositWalletAddress,
            nonce = nonce,
            deadline = deadline,
            calls = calls,
            signature = signature,
        )

        return redeemRepository.submit(walletId = params.walletId, batch = batch)
            .getOrElse { error ->
                if (error is PolymarketRedeemError.Network) {
                    redeemRepository.submit(walletId = params.walletId, batch = batch)
                        .getOrElse { raise(PredictionClaimFailure.Rejected(it)) }
                } else {
                    raise(PredictionClaimFailure.Rejected(error))
                }
            }
    }

    /**
     * The position's version is the one the redeem calls are checked against. The copy the screen holds may lack
     * it, or predate a correction on the backend's side; in both cases the position is read again and the fresh
     * copy decides, so that a stale list cannot refuse a correct answer.
     */
    private suspend fun Raise<PredictionClaimFailure>.resolveVersion(
        params: Params,
        served: PolymarketMarketVersion?,
    ): PolymarketMarketVersion {
        val known = params.version
        if (known != null && (served == null || served == known)) return known
        val fresh = positionsRepository.getPosition(
            addresses = PolymarketAddresses(
                ownerAddress = params.ownerAddress,
                depositWalletAddress = params.depositWalletAddress,
                userWalletId = params.userWalletId,
            ),
            id = PolymarketPositionId(conditionId = params.conditionId, assetId = params.assetId),
        ).getOrElse { raise(PredictionClaimFailure.VersionUnavailable(detail = it.toString())) }
        return fresh?.version
            ?: known
            ?: raise(PredictionClaimFailure.VersionUnavailable(detail = "Position has no market version"))
    }

    private suspend fun Raise<PredictionClaimFailure>.settle(
        params: Params,
        submitted: PredictionClaimReceipt,
    ): PredictionClaimReceipt {
        var latest = submitted
        repeat(POLL_ATTEMPTS) {
            if (latest.state.isTerminal) return terminal(latest)
            delay(POLL_INTERVAL)
            latest = redeemRepository.getState(
                walletId = params.walletId,
                transactionId = submitted.transactionId,
            ).getOrElse { error ->
                if (error.endsPolling()) raise(PredictionClaimFailure.Unconfirmed(latest))
                return@repeat
            }
        }
        return if (latest.state.isTerminal) terminal(latest) else raise(PredictionClaimFailure.Unconfirmed(latest))
    }

    private fun PolymarketRedeemError.endsPolling(): Boolean =
        this is PolymarketRedeemError.NotFound || this is PolymarketRedeemError.Unauthorized

    private fun Raise<PredictionClaimFailure>.terminal(receipt: PredictionClaimReceipt): PredictionClaimReceipt =
        if (receipt.state == PredictionClaimState.FAILED) raise(PredictionClaimFailure.Failed(receipt)) else receipt

    /**
     * @property assetId the position's token, which the redeem calls must name on a V2 market
     * @property version the position's market version as the BFF served it; `null` is re-read before signing
     */
    data class Params(
        val userWalletId: UserWalletId,
        val walletId: String,
        val ownerAddress: String,
        val depositWalletAddress: String,
        val conditionId: String,
        val assetId: String,
        val isNegRisk: Boolean,
        val version: PolymarketMarketVersion?,
    )

    private companion object {
        const val POLL_ATTEMPTS = 20
        val POLL_INTERVAL = 3.seconds
    }
}