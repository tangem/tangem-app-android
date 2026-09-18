package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

sealed interface PredictionClaimFailure {

    data object CallUnavailable : PredictionClaimFailure

    data object NonceUnavailable : PredictionClaimFailure

    data class SigningFailed(val error: PolymarketSigningError) : PredictionClaimFailure

    data object Expired : PredictionClaimFailure

    data class Rejected(val error: PolymarketRedeemError) : PredictionClaimFailure

    /** The claim was rejected on chain, or the backend stopped following it. Nothing left the account. */
    data class Failed(val receipt: PredictionClaimReceipt) : PredictionClaimFailure

    /**
     * The claim did not settle within the screen's waiting window. It may still land — refresh the
     * positions before telling the user anything.
     */
    data class Unconfirmed(val receipt: PredictionClaimReceipt) : PredictionClaimFailure
}

class SubmitPredictionClaimInteractor(
    private val redeemRepository: PolymarketRedeemRepository,
    private val polymarketRepository: PolymarketRepository,
    private val signer: PolymarketTypedDataSigner,
) {

    /**
     * @return the settled receipt of the claim; the backend keys a claim on (wallet, market), so a resumed
     *  screen or a repeated tap converges on the claim already in flight rather than relaying a second one
     */
    suspend fun claim(params: Params): Either<PredictionClaimFailure, PredictionClaimReceipt> = either {
        settle(params, submit(params))
    }

    private suspend fun Raise<PredictionClaimFailure>.submit(params: Params): PredictionClaimReceipt {
        val call = redeemRepository.getCall(
            walletId = params.walletId,
            conditionId = params.conditionId,
            isNegRisk = params.isNegRisk,
        ).getOrElse { raise(PredictionClaimFailure.CallUnavailable) }

        val nonce = polymarketRepository.getRelayerNonce(params.ownerAddress)
            .getOrElse { raise(PredictionClaimFailure.NonceUnavailable) }
            .toString()

        val deadline = PolymarketRelayerDeadline.fromNow()
        ensure(!PolymarketRelayerDeadline.isExpired(deadline)) { PredictionClaimFailure.Expired }

        val signature = signer.signRelayedBatch(
            userWalletId = params.userWalletId,
            payload = PolymarketApprovalsPayload(
                depositWalletAddress = params.depositWalletAddress,
                nonce = nonce,
                deadline = deadline,
                calls = listOf(call),
            ),
        ).getOrElse { raise(PredictionClaimFailure.SigningFailed(it)) }

        val batch = PredictionClaimBatch(
            ownerAddress = params.ownerAddress,
            depositWalletAddress = params.depositWalletAddress,
            nonce = nonce,
            deadline = deadline,
            call = call,
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
                if (error.endsPolling()) raise(PredictionClaimFailure.Rejected(error))
                return@repeat
            }
        }
        return if (latest.state.isTerminal) terminal(latest) else raise(PredictionClaimFailure.Unconfirmed(latest))
    }

    private fun PolymarketRedeemError.endsPolling(): Boolean =
        this is PolymarketRedeemError.NotFound || this is PolymarketRedeemError.Unauthorized

    private fun Raise<PredictionClaimFailure>.terminal(receipt: PredictionClaimReceipt): PredictionClaimReceipt =
        if (receipt.state == PredictionClaimState.FAILED) raise(PredictionClaimFailure.Failed(receipt)) else receipt

    data class Params(
        val userWalletId: UserWalletId,
        val walletId: String,
        val ownerAddress: String,
        val depositWalletAddress: String,
        val conditionId: String,
        val isNegRisk: Boolean,
    )

    private companion object {
        const val POLL_ATTEMPTS = 20
        val POLL_INTERVAL = 3.seconds
    }
}