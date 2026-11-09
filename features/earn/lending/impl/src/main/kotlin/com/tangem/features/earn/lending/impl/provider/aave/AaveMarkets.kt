package com.tangem.features.earn.lending.impl.provider.aave

import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AaveMarkets @Inject constructor(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    /** The WETH market from the yield backend; null until the backend lists it */
    fun observe(asset: LendingAsset): Flow<YieldMarketToken?> {
        val key = asset.weth.yieldSupplyKey()
        return yieldSupplyRepository.getMarketsFlow()
            .map { markets -> markets.firstOrNull { it.yieldSupplyKey == key } }
    }

    suspend fun current(asset: LendingAsset): YieldMarketToken? = observe(asset).first()
}