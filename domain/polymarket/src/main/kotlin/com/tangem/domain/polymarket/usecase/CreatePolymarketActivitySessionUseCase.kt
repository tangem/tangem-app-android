package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.session.PolymarketActivitySession
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class CreatePolymarketActivitySessionUseCase(
    private val repository: PolymarketActivityRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWalletId: UserWalletId): PolymarketActivitySession = PolymarketActivitySession(
        userWalletId = userWalletId,
        repository = repository,
        deriveAddresses = deriveAddresses,
        dispatchers = dispatchers,
    )
}