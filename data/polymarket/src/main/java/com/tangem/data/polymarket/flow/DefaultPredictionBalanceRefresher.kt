package com.tangem.data.polymarket.flow

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionBalanceRefresher
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultPredictionBalanceRefresher @Inject constructor(
    private val statusStore: PredictionAccountStatusStore,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val statusFetcher: PredictionAccountStatusFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionBalanceRefresher {

    override suspend fun refresh(userWalletId: UserWalletId): Either<Throwable, Unit> {
        val isActive = statusStore.getSyncOrNull(userWalletId = userWalletId) is PredictionAccountStatusValue.Active
        if (!isActive) {
            return statusFetcher(PredictionAccountStatusFetcher.Params(userWalletId = userWalletId))
        }

        return Either.catchOn(dispatchers.default) {
            val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId)
                ?: error("No stored prediction addresses for $userWalletId")

            val balanceAllowance = getPolymarketBalanceInteractor(addresses = addresses)
                .getOrElse { error("Prediction balance is unavailable: $it") }

            val current = statusStore.getSyncOrNull(userWalletId = userWalletId) as? PredictionAccountStatusValue.Active
                ?: return@catchOn

            statusStore.store(
                userWalletId = userWalletId,
                value = current.copy(balance = balanceAllowance.balance, source = StatusSource.ACTUAL),
            )
        }.onLeft { logger.e("Failed to refresh the prediction balance of $userWalletId", it) }
    }

    private companion object {
        val logger = TangemLogger.withTag("PredictionBalanceRefresher")
    }
}