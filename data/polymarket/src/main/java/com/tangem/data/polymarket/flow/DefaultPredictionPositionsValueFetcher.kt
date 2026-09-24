package com.tangem.data.polymarket.flow

import arrow.core.Either
import com.tangem.data.polymarket.store.PredictionPositionsValueStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionPositionsValueFetcher
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultPredictionPositionsValueFetcher @Inject constructor(
    private val store: PredictionPositionsValueStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionPositionsValueFetcher {

    override suspend fun invoke(params: PredictionPositionsValueFetcher.Params): Either<Throwable, Unit> {
        val userWalletId = params.userWalletId

        return Either.catchOn(dispatchers.default) {
            val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)
            if (userWallet == null || userWallet.isLocked) {
                store.markUnrefreshed(userWalletId = userWalletId)
                return@catchOn
            }

            val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = userWalletId) ?: return@catchOn

            positionsRepository.getPositionsValue(addresses = addresses).fold(
                ifLeft = { error ->
                    logger.e("Prediction positions value is unavailable for $userWalletId: $error")
                    store.markUnrefreshed(userWalletId = userWalletId)
                },
                ifRight = { amount ->
                    store.store(
                        userWalletId = userWalletId,
                        value = PredictionPositionsValue.Known(amount = amount, source = StatusSource.ACTUAL),
                    )
                },
            )
        }.onLeft { error ->
            logger.e("Failed to refresh the prediction positions value of $userWalletId", error)
            store.markUnrefreshed(userWalletId = userWalletId)
        }
    }

    private companion object {
        val logger = TangemLogger.withTag("PredictionPositionsValueFetcher")
    }
}