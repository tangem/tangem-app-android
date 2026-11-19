package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCryptoPriceRepository
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import kotlinx.coroutines.flow.Flow

class ObservePolymarketCryptoPricesUseCase(private val repository: PolymarketCryptoPriceRepository) {

    operator fun invoke(
        userWalletId: UserWalletId,
        feed: PolymarketCryptoPriceFeed,
    ): Flow<PolymarketCryptoPriceSeries> = repository.observePrices(userWalletId = userWalletId, feed = feed)
}