package com.tangem.data.polymarket.flow

import arrow.core.Either
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
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
import javax.inject.Inject

/**
 * Refreshes the prediction account status of one wallet into the store.
 *
 * It is driven by the wallet screen, so it runs for wallets that never opened the feature. It therefore reads the
 * addresses from what this device has already derived: deriving them for real opens a card session or unlocks the
 * wallet, and a background refresh may not ask the user for either. No stored addresses means the account has not
 * been set up on this device, which is reported as not onboarded rather than as an error.
 *
 * A failure leaves the cached status in place and marks it as un-refreshed, so a refresh that could not reach the
 * backend does not erase the balance the user was looking at.
 */
@Suppress("LongParameterList")
internal class DefaultPredictionAccountStatusFetcher @Inject constructor(
    private val statusStore: PredictionAccountStatusStore,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionAccountStatusFetcher {

    override suspend fun invoke(params: PredictionAccountStatusFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val value = resolve(userWalletId = params.userWalletId)

            statusStore.store(userWalletId = params.userWalletId, value = value)
        }.onLeft {
            statusStore.updateStatusSource(userWalletId = params.userWalletId, source = StatusSource.ONLY_CACHE)
        }
    }

    private suspend fun resolve(userWalletId: UserWalletId): PredictionAccountStatusValue {
        val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId)
            ?: return PredictionAccountStatusValue.NotOnboarded

        val state = getPolymarketWalletStatusUseCase(addresses = addresses).getOrNull()
            ?: return PredictionAccountStatusValue.Error.Unavailable

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
            PolymarketWalletStatus.UNKNOWN -> PredictionAccountStatusValue.Error.Unavailable
            PolymarketWalletStatus.READY_TO_TRADE -> active(addresses = addresses)
        }
    }

    /**
     * The balance read needs the L2 credentials this device holds. A wallet deployed elsewhere has none here, and
     * that is not an error — its setup is simply unfinished on this device, which is what the deployed stage says.
     */
    private suspend fun active(addresses: PolymarketAddresses): PredictionAccountStatusValue {
        singleQuoteStatusFetcher(
            SingleQuoteStatusFetcher.Params(rawCurrencyId = COLLATERAL_CURRENCY_ID, appCurrencyId = null),
        )

        return getPolymarketBalanceInteractor(addresses = addresses).fold(
            ifLeft = { error ->
                when (error) {
                    is PolymarketAuthError.KeyNotFound -> onboarding(
                        PredictionAccountStatusValue.Onboarding.Stage.DEPLOYED,
                    )
                    else -> PredictionAccountStatusValue.Error.Unavailable
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
        return checkPolymarketGeoblockUseCase().fold(ifLeft = { true }, ifRight = { isBlocked -> !isBlocked })
    }

    private fun onboarding(stage: PredictionAccountStatusValue.Onboarding.Stage) =
        PredictionAccountStatusValue.Onboarding(source = StatusSource.ACTUAL, stage = stage)
}