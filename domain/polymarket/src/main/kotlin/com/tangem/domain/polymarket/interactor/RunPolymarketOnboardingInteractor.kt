package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DeployDepositWalletUseCase
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.SignOnboardingDigestsUseCase
import com.tangem.domain.polymarket.usecase.SubmitApprovalsUseCase
import com.tangem.domain.polymarket.usecase.SyncBalanceAllowanceUseCase
import com.tangem.domain.polymarket.usecase.isRetryable
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/**
 * Drives a Polymarket onboarding run to completion, resuming from the status the backend currently reports.
 * The card is used for at most one signing session per run: this use case never asks for a second signature,
 * it reports a retryable failure and lets the caller start a new run.
 *
 * The run has no deadline of its own. While the backend reports a non-terminal status the poll continues; the
 * only way it stops waiting is a terminal status, a non-network error, or repeated network failures.
 */
@Suppress("LongParameterList")
class RunPolymarketOnboardingInteractor(
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getWalletStatus: GetPolymarketWalletStatusUseCase,
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase,
    private val signOnboardingDigests: SignOnboardingDigestsUseCase,
    private val deployDepositWallet: DeployDepositWalletUseCase,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val deriveApiCredentials: DeriveApiCredentialsUseCase,
    private val submitApprovals: SubmitApprovalsUseCase,
    private val syncBalanceAllowance: SyncBalanceAllowanceUseCase,
    private val polymarketOnboardedStore: PolymarketOnboardedStore,
    private val checkGeoblock: CheckPolymarketGeoblockUseCase,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<PolymarketOnboardingProgress> = flow {
        emit(PolymarketOnboardingProgress.Deriving)
        val addresses = step { deriveAddresses(userWalletId) } ?: return@flow
        runOnboarding(addresses)
    }.onEach { progress -> logProgress(progress) }
        .catch { throwable ->
            TangemLogger.e("Onboarding run failed", throwable)
            emit(PolymarketOnboardingProgress.Failed(error = PolymarketOnboardingError.Unknown, isRetryable = true))
        }

    private fun logProgress(progress: PolymarketOnboardingProgress) {
        if (progress is PolymarketOnboardingProgress.Failed) {
            TangemLogger.e("Onboarding progress: $progress")
        } else {
            TangemLogger.i("Onboarding progress: $progress")
        }
    }

    private suspend fun FlowCollector<PolymarketOnboardingProgress>.runOnboarding(addresses: PolymarketAddresses) {
        val entry = (step { getWalletStatus(addresses) } ?: return).status
        val credentials = getApiCredentials(addresses.userWalletId)

        if (!entry.owesApprovals() && credentials != null) {
            awaitStatus(addresses, PolymarketWalletStatus.READY_TO_TRADE, from = entry) ?: return
            finish(addresses, credentials)
            return
        }

        if (entry.needsDeploy() && !isRegionAllowed()) return

        emit(PolymarketOnboardingProgress.AwaitingSignature)
        val nonce = step { getRelayerNonce(addresses) } ?: return
        val signed = step { signOnboardingDigests(addresses, nonce) } ?: return

        settleWallet(addresses = addresses, entry = entry, signed = signed, credentials = credentials)
    }

    /**
     * Whether the region permits opening a new account. Read fresh and fail-closed — an unknown region must
     * not deploy. Existing accounts are untouched by it, so only [needsDeploy] runs are asked.
     */
    private suspend fun FlowCollector<PolymarketOnboardingProgress>.isRegionAllowed(): Boolean {
        val isBlocked = checkGeoblock().getOrElse { error ->
            TangemLogger.e("Onboarding: region check failed, deploy refused: $error")
            true
        }

        if (isBlocked) {
            emit(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.RegionBlocked,
                    isRetryable = false,
                ),
            )
        }

        return !isBlocked
    }

    private suspend fun FlowCollector<PolymarketOnboardingProgress>.settleWallet(
        addresses: PolymarketAddresses,
        entry: PolymarketWalletStatus,
        signed: PolymarketSignedOnboarding,
        credentials: PolymarketApiCredentials?,
    ) {
        var current = entry
        if (entry.needsDeploy()) {
            current = step { deployDepositWallet(addresses) } ?: return
        }

        val activeCredentials = credentials ?: step {
            deriveApiCredentials(
                userWalletId = addresses.userWalletId,
                ownerAddress = addresses.ownerAddress,
                l1Signature = signed.l1Signature,
                timestamp = signed.clobAuthTimestamp,
            )
        } ?: return

        if (entry.owesApprovals()) {
            if (!entry.isDeployComplete()) {
                current = awaitStatus(addresses, PolymarketWalletStatus.DEPLOYED, from = current) ?: return
            }
            current = step { submitApprovals(addresses, signed) } ?: return
        }

        awaitStatus(addresses, PolymarketWalletStatus.READY_TO_TRADE, from = current) ?: return
        finish(addresses, activeCredentials)
    }

    private suspend fun FlowCollector<PolymarketOnboardingProgress>.awaitStatus(
        addresses: PolymarketAddresses,
        target: PolymarketWalletStatus,
        from: PolymarketWalletStatus,
    ): PolymarketWalletStatus? {
        if (from == target) return from

        var reported = from
        var consecutiveFailures = 0
        TangemLogger.v("Poll: waiting for $target, from $from")
        emit(PolymarketOnboardingProgress.Working(from))

        while (true) {
            val state = getWalletStatus(addresses).fold(
                ifLeft = { error ->
                    if (error != PolymarketOnboardingError.Network) {
                        emit(PolymarketOnboardingProgress.Failed(error, isRetryable = error.isRetryable()))
                        return null
                    }
                    consecutiveFailures++
                    if (consecutiveFailures >= MAX_CONSECUTIVE_POLL_FAILURES) {
                        emit(PolymarketOnboardingProgress.Failed(error, isRetryable = error.isRetryable()))
                        return null
                    }
                    null
                },
                ifRight = { walletState ->
                    consecutiveFailures = 0
                    walletState
                },
            )

            val status = state?.status
            if (status == target) return status
            if (status != null) {
                status.toFailure()?.let { failure ->
                    emit(PolymarketOnboardingProgress.Failed(failure, isRetryable = failure.isRetryable()))
                    return null
                }
                if (status != reported) {
                    TangemLogger.v("Poll: status changed to $status")
                    emit(PolymarketOnboardingProgress.Working(status))
                    reported = status
                }
            }

            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private suspend fun FlowCollector<PolymarketOnboardingProgress>.finish(
        addresses: PolymarketAddresses,
        credentials: PolymarketApiCredentials,
    ) {
        polymarketOnboardedStore.markOnboarded(addresses.userWalletId)
        primeBalanceCache(addresses, credentials)
        emit(PolymarketOnboardingProgress.Ready)
    }

    /** Best-effort refresh of the CLOB's cached balance and allowance; onboarding is complete either way. */
    private suspend fun primeBalanceCache(addresses: PolymarketAddresses, credentials: PolymarketApiCredentials) {
        try {
            syncBalanceAllowance(ownerAddress = addresses.ownerAddress, credentials = credentials)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ignored: Exception) {
            return
        }
    }

    private suspend fun <T> FlowCollector<PolymarketOnboardingProgress>.step(
        block: suspend () -> Either<PolymarketOnboardingError, T>,
    ): T? = block().fold(
        ifLeft = { error ->
            TangemLogger.e("Onboarding step failed: $error")
            emit(PolymarketOnboardingProgress.Failed(error = error, isRetryable = error.isRetryable()))
            null
        },
        ifRight = { it },
    )

    private fun PolymarketWalletStatus.owesApprovals(): Boolean = when (this) {
        PolymarketWalletStatus.READY_TO_TRADE,
        PolymarketWalletStatus.APPROVALS_IN_PROGRESS,
        -> false
        else -> true
    }

    private fun PolymarketWalletStatus.needsDeploy(): Boolean = this == PolymarketWalletStatus.NOT_CREATED ||
        this == PolymarketWalletStatus.DEPLOYMENT_FAILED

    private fun PolymarketWalletStatus.isDeployComplete(): Boolean =
        this == PolymarketWalletStatus.DEPLOYED || this == PolymarketWalletStatus.APPROVALS_FAILED

    private fun PolymarketWalletStatus.toFailure(): PolymarketOnboardingError? = when (this) {
        PolymarketWalletStatus.DEPLOYMENT_FAILED -> PolymarketOnboardingError.DeploymentFailed
        PolymarketWalletStatus.APPROVALS_FAILED -> PolymarketOnboardingError.ApprovalsFailed
        else -> null
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 2_500L
        const val MAX_CONSECUTIVE_POLL_FAILURES = 3
    }
}