package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Emits the markets entry of [token] and follows it, so a surface built on the campaign recovers once a cold
 * cache warms up instead of staying on whatever the cache held when the screen opened.
 *
 * Null while the cache has no entry for the token. Deliberately no single-token fallback: that endpoint carries
 * no campaign, so it would answer "no campaign" rather than "not known yet".
 */
class YieldSupplyMarketFlowUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(token: CryptoCurrency.Token): Flow<YieldMarketToken?> = yieldSupplyRepository
        .getMarketsFlow()
        .map { markets -> markets.firstOrNull { it.yieldSupplyKey == token.yieldSupplyKey() } }
        .distinctUntilChanged()
}