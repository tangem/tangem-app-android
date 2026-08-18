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
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

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
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionAccountStatusFetcher {

    override suspend fun invoke(params: PredictionAccountStatusFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            // Unconditionally: a cached balance needs the rate as much as a freshly read one, and every path that
            // returns early below leaves that balance in place. Without the quote the producer keeps reporting
            // loading, which contributes zero, so the collateral would silently read as nothing.
            singleQuoteStatusFetcher(
                SingleQuoteStatusFetcher.Params(rawCurrencyId = COLLATERAL_CURRENCY_ID, appCurrencyId = null),
            )

            val value = resolve(userWalletId = params.userWalletId)

            if (value == null) {
                markUnrefreshed(userWalletId = params.userWalletId)
            } else {
                statusStore.store(userWalletId = params.userWalletId, value = value)
            }
        }.onLeft { error ->
            logger.e("Failed to refresh the prediction account of ${params.userWalletId}", error)
            markUnrefreshed(userWalletId = params.userWalletId)
        }
    }

    /**
     * The status if it could be established, `null` if it could not — in which case the caller keeps whatever is
     * cached instead of replacing it with a state that claims the account is empty or absent.
     */
    private suspend fun resolve(userWalletId: UserWalletId): PredictionAccountStatusValue? {
        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return null
        // A locked wallet holds no keys to read the addresses from, which says nothing about the account behind them
        if (userWallet.isLocked) return null

        val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId)
            ?: return PredictionAccountStatusValue.NotOnboarded

        val state = getPolymarketWalletStatusUseCase(addresses = addresses)
            .onLeft { logger.e("Prediction wallet status is unavailable for $userWalletId: $it") }
            .getOrNull()
            ?: return null

        return when (state.status) {
            PolymarketWalletStatus.NOT_CREATED -> PredictionAccountStatusValue.NotOnboarded
            PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS -> onboarding(
                PredictionAccountStatusValue.Onboarding.Stage.DEPLOYING,
            )
            PolymarketWalletStatus.DEPLOYED -> onboarding(PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED)
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS -> onboarding(
                PredictionAccountStatusValue.Onboarding.Stage.APPROVING,
            )
            PolymarketWalletStatus.DEPLOYMENT_FAILED,
            PolymarketWalletStatus.APPROVALS_FAILED,
            -> PredictionAccountStatusValue.Error.OnboardingFailed
            // A status this build does not know is "not ready, keep polling" by the BFF contract, not an error
            PolymarketWalletStatus.UNKNOWN -> null
            PolymarketWalletStatus.READY_TO_TRADE -> active(addresses = addresses)
        }
    }

    /**
     * The balance read needs the L2 credentials this device holds. A wallet deployed elsewhere has none here, and
     * that is not an error — its setup is simply unfinished on this device, which is what the deployed stage says.
     */
    private suspend fun active(addresses: PolymarketAddresses): PredictionAccountStatusValue? {
        return getPolymarketBalanceInteractor(addresses = addresses).fold(
            ifLeft = { error ->
                when (error) {
                    is PolymarketAuthError.KeyNotFound -> onboarding(
                        PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
                    )
                    // Throttled, offline or rejected: the balance is unknown, not zero
                    else -> {
                        logger.e("Prediction collateral is unavailable: $error")
                        null
                    }
                }
            },
            ifRight = { balanceAllowance ->
                PredictionAccountStatusValue.Active(
                    source = StatusSource.ACTUAL,
                    balance = balanceAllowance.balance,
                    fiatRate = null,
                    isTradingAllowed = isTradingAllowed(),
                )
            },
        )
    }

    /**
     * A region check that fails is not treated as a block: the screens that let the user trade run their own check
     * before anything can be sent, so guessing "blocked" here would only hide a balance the user does own.
     */
    private suspend fun isTradingAllowed(): Boolean {
        return checkPolymarketGeoblockUseCase().fold(
            ifLeft = { error ->
                logger.e("Prediction region check failed, trading is left allowed: $error")
                true
            },
            ifRight = { isBlocked -> !isBlocked },
        )
    }

    private fun onboarding(stage: PredictionAccountStatusValue.Onboarding.Stage) =
        PredictionAccountStatusValue.Onboarding(source = StatusSource.ACTUAL, stage = stage)

    /** Guarded: the likeliest reason to be here is that the store itself failed, and this asks it to write again. */
    private suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        runSuspendCatching {
            statusStore.updateStatusSource(userWalletId = userWalletId, source = StatusSource.ONLY_CACHE)
        }.onFailure { logger.e("Failed to mark the prediction account of $userWalletId as un-refreshed", it) }
    }

    private companion object {
        val logger = TangemLogger.withTag("PredictionAccountStatusFetcher")
    }
}