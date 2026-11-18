package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import kotlinx.coroutines.flow.Flow

interface PolymarketCryptoPriceRepository {

    /**
     * Live prices of the [feed]'s coin, starting with what the stream remembers of the last two minutes. The stream
     * is opened with the API credentials of [userWalletId] and is unavailable without them. The connection stays
     * open only while the flow is collected.
     */
    fun observePrices(userWalletId: UserWalletId, feed: PolymarketCryptoPriceFeed): Flow<PolymarketCryptoPriceSeries>
}