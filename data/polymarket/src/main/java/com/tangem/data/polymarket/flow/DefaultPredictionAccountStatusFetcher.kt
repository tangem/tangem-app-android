package com.tangem.data.polymarket.flow

import arrow.core.Either
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher.Mode
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.RecordPolymarketConfirmationUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import javax.inject.Inject

private typealias PredictionAccountStatusMerge = (PredictionAccountStatusValue?) -> PredictionAccountStatusValue

/**
 * Refreshes the prediction account status of one wallet into the store.
 *
 * It is driven by the wallet screen, so it runs for wallets that never opened the feature. It therefore reads the
 * addresses from what this device has already derived: deriving them for real opens a card session or unlocks the
 * wallet, and a background refresh may not ask the user for either. No stored addresses means the account has not
 * been set up on this device, which is reported as not onboarded rather than as an error.
 *
 * Anything it could not find out — an unreachable backend, a locked wallet, a throttled CLOB — leaves the cached
 * status in place and marks it as un-refreshed. It never writes a state that means "there is nothing here" on the
 * strength of a failed question: the repository answers with `Either.Left` rather than throwing, so treating a left
 * as an answer would drop a real balance out of the wallet total and out of the on-disk cache.
 */
@Suppress("LongParameterList")
internal class DefaultPredictionAccountStatusFetcher @Inject constructor(
    private val statusStore: PredictionAccountStatusStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    private val polymarketRepository: PolymarketRepository,
    private val positionsRepository: PolymarketPositionsRepository,
    private val recordPolymarketConfirmation: RecordPolymarketConfirmationUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionAccountStatusFetcher {

    override suspend fun invoke(params: PredictionAccountStatusFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val merge = resolve(userWalletId = params.userWalletId, mode = params.mode)

            if (merge == null) {
                markUnrefreshed(userWalletId = params.userWalletId)
            } else {
                statusStore.update(userWalletId = params.userWalletId, transform = merge)
            }
        }.onLeft { error ->
            logger.e("Failed to refresh the prediction account of ${params.userWalletId}", error)
            markUnrefreshed(userWalletId = params.userWalletId)
        }
    }

    /**
     * The merge to commit if the status could be established, `null` if it could not — in which case the caller
     * keeps whatever is cached instead of replacing it with a state that claims the account is empty or absent.
     */
    private suspend fun resolve(userWalletId: UserWalletId, mode: Mode): PredictionAccountStatusMerge? {
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return null
        // A locked wallet holds no keys to read the addresses from, which says nothing about the account behind them
        if (userWallet.isLocked) return null

        val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId)
            ?: return resolveWithoutAddresses(userWalletId = userWalletId)

        val state = getPolymarketWalletStatusUseCase(addresses = addresses)
            .onLeft { logger.e("Prediction wallet status is unavailable for $userWalletId: $it") }
            .getOrNull()
            ?: return null

        return toStatusValue(userWalletId = userWalletId, status = state.status) {
            active(addresses = addresses, mode = mode)
        }
    }

    /**
     * Shared by both reads so they cannot disagree about a status. [whenReady] supplies the ready state, which
     * differs by path: the balance needs credentials only the addressed path has.
     */
    private suspend fun toStatusValue(
        userWalletId: UserWalletId,
        status: PolymarketWalletStatus,
        whenReady: suspend () -> PredictionAccountStatusMerge?,
    ): PredictionAccountStatusMerge? {
        val value = when (status) {
            PolymarketWalletStatus.NOT_CREATED -> constant(PredictionAccountStatusValue.NotOnboarded)
            PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS -> onboarding(
                PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING,
            )
            PolymarketWalletStatus.DEPLOYED -> onboarding(PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED)
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS -> onboarding(
                PredictionAccountStatusValue.Onboarding.Stage.APPROVING,
            )
            PolymarketWalletStatus.DEPLOYMENT_FAILED,
            PolymarketWalletStatus.APPROVALS_FAILED,
            -> constant(PredictionAccountStatusValue.Error.OnboardingFailed)
            // A status this build does not know is "not ready, keep polling" by the BFF contract, not an error
            PolymarketWalletStatus.UNKNOWN -> null
            PolymarketWalletStatus.READY_TO_TRADE -> whenReady()
        }

        // Keyed on the status, not on the value: a recognised status is an answer about the account even when
        // the balance behind it could not be read.
        recordPolymarketConfirmation(userWalletId = userWalletId, status = status)

        return value
    }

    /**
     * A missing key is not an unfinished setup: the backend has just called the wallet ready, so what is unknown is
     * the collateral, not the account. The resolver also answers `KeyNotFound` for any `404` from the CLOB — all the
     * more reason to withhold the wallet total here rather than contribute a zero to it.
     */
    private suspend fun active(addresses: PolymarketAddresses, mode: Mode): PredictionAccountStatusMerge? =
        coroutineScope {
            val readPositionsValue = async {
                if (mode == Mode.FULL) readPositionsValue(addresses = addresses) else null
            }

            getPolymarketBalanceInteractor(addresses = addresses).fold(
                ifLeft = { error ->
                    readPositionsValue.cancel()
                    when (error) {
                        is PolymarketAuthError.KeyNotFound -> constant(
                            PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
                        )
                        // Throttled, offline or rejected: the balance is unknown, not zero
                        else -> {
                            logger.e("Prediction collateral is unavailable: $error")
                            null
                        }
                    }
                },
                ifRight = { balanceAllowance ->
                    activeMerge(
                        balance = balanceAllowance.balance,
                        positionsValue = readPositionsValue.await(),
                        mode = mode,
                    )
                },
            )
        }

    /**
     * @param positionsValue the value read by this refresh, `null` when it was not read or could not be read
     */
    private fun activeMerge(
        balance: BigDecimal,
        positionsValue: BigDecimal?,
        mode: Mode,
    ): PredictionAccountStatusMerge = { current ->
        val stored = current as? PredictionAccountStatusValue.Active

        PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = balance,
            positionsValue = when (mode) {
                Mode.FULL -> positionsValue ?: stored?.positionsValue
                Mode.AVAILABLE_ONLY -> stored?.positionsValue
            },
            fiatRate = null,
            depositCurrency = collateralCurrencyFactory.createDepositTarget(),
            depositAddress = stored?.depositAddress,
        )
    }

    private suspend fun readPositionsValue(addresses: PolymarketAddresses): BigDecimal? =
        positionsRepository.getPositionsValue(addresses = addresses)
            .onLeft { logger.e("Prediction positions value is unavailable: $it") }
            .getOrNull()

    /** A wallet this device never derived an address for: the backend still knows it by its Tangem wallet id. */
    private suspend fun resolveWithoutAddresses(userWalletId: UserWalletId): PredictionAccountStatusMerge? {
        val state = polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue)
            .onLeft { logger.e("Prediction wallet status by id is unavailable for $userWalletId: $it") }
            .getOrNull()
            ?: return null

        return toStatusValue(userWalletId = userWalletId, status = state.status) {
            constant(PredictionAccountStatusValue.Onboarded(source = StatusSource.ACTUAL))
        }
    }

    private fun onboarding(stage: PredictionAccountStatusValue.Onboarding.Stage) =
        constant(PredictionAccountStatusValue.Onboarding(source = StatusSource.ACTUAL, stage = stage))

    private fun constant(value: PredictionAccountStatusValue): PredictionAccountStatusMerge = { value }

    /** Guarded: the likeliest reason to be here is that the store itself failed, and this asks it to write again. */
    private suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        runSuspendCatching { statusStore.markUnrefreshed(userWalletId = userWalletId) }
            .onFailure { logger.e("Failed to mark the prediction account of $userWalletId as un-refreshed", it) }
    }

    private companion object {
        val logger = TangemLogger.withTag("PredictionAccountStatusFetcher")
    }
}