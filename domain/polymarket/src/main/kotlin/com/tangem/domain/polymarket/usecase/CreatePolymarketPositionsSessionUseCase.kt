package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateListener
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.session.PolymarketPositionsSession
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class CreatePolymarketPositionsSessionUseCase(
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val updateListener: PredictionPositionsUpdateListener,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId, tab: PolymarketPositionsTab): PolymarketPositionsSession =
        PolymarketPositionsSession(
            userWalletId = userWalletId,
            tab = tab,
            repository = repository,
            deriveAddresses = deriveAddresses,
            updateListener = updateListener,
            dispatchers = dispatchers,
        )
}