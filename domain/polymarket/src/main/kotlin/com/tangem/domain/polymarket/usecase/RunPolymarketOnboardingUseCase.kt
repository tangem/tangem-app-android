package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Drives a Polymarket onboarding run to completion, resuming from the status the backend currently reports.
 * The card is used at most once per run: this use case never asks for a second signature, it reports a
 * retryable failure and lets the caller start a new run.
 */
@Suppress("LongParameterList")
class RunPolymarketOnboardingUseCase(
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getWalletStatus: GetPolymarketWalletStatusUseCase,
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase,
    private val signOnboardingDigests: SignOnboardingDigestsUseCase,
    private val deployDepositWallet: DeployDepositWalletUseCase,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val deriveApiCredentials: DeriveApiCredentialsUseCase,
    private val submitApprovals: SubmitApprovalsUseCase,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketOnboardingProgress> = flow {
        emit(PolymarketOnboardingProgress.Deriving)
        val addresses = step { deriveAddresses(userWalletId) } ?: return@flow
        val state = step { getWalletStatus(addresses) } ?: return@flow

        emit(PolymarketOnboardingProgress.AwaitingSignature)
        val nonce = step { getRelayerNonce(addresses) } ?: return@flow
        val signed = step { signOnboardingDigests(addresses, nonce) } ?: return@flow

        val deployed = step { deployDepositWallet(addresses) } ?: return@flow

        if (getApiCredentials(addresses.ownerAddress) == null) {
            step {
                deriveApiCredentials(
                    ownerAddress = addresses.ownerAddress,
                    l1Signature = signed.l1Signature,
                    timestamp = signed.clobAuthTimestamp,
                )
            } ?: return@flow
        }

        awaitStatus(addresses, target = PolymarketWalletStatus.DEPLOYED, from = deployed) ?: return@flow
        val submitted = step { submitApprovals(addresses, signed) } ?: return@flow
        awaitStatus(addresses, target = PolymarketWalletStatus.READY_TO_TRADE, from = submitted)
            ?: return@flow

        emit(PolymarketOnboardingProgress.Ready)
    }

    private suspend fun FlowCollector<PolymarketOnboardingProgress>.awaitStatus(
        addresses: PolymarketAddresses,
        target: PolymarketWalletStatus,
        from: PolymarketWalletStatus,
    ): PolymarketWalletStatus? {
        var reported = from
        var waited = 0L
        emit(PolymarketOnboardingProgress.Working(from))

        while (waited < POLL_CEILING_MILLIS) {
            val state = step { getWalletStatus(addresses) } ?: return null
            val status = state.status

            if (status == target) return status
            if (status != reported) {
                emit(PolymarketOnboardingProgress.Working(status))
                reported = status
            }

            delay(POLL_INTERVAL_MILLIS)
            waited += POLL_INTERVAL_MILLIS
        }

        emit(PolymarketOnboardingProgress.StillWorking(reported))
        return null
    }

    private suspend fun <T> FlowCollector<PolymarketOnboardingProgress>.step(
        block: suspend () -> Either<PolymarketOnboardingError, T>,
    ): T? = block().fold(
        ifLeft = { error ->
            emit(PolymarketOnboardingProgress.Failed(error = error, retryable = error.isRetryable()))
            null
        },
        ifRight = { it },
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 2_500L
        const val POLL_CEILING_MILLIS = 120_000L
    }
}