package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AaveMarkets @Inject constructor(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    private val updateMutex = Mutex()

    @Volatile
    private var updateCount = 0

    private var lastUpdateSource = StatusSource.CACHE

    @Volatile
    private var isListUpdated = false

    /** The stored list may be stale: a missing market means "not offered" only after this process updates it */
    fun observeRead(asset: LendingAsset): Flow<AaveMarketRead> = flow {
        val countBeforeCheck = updateCount
        val source = if (current(asset) == null && !isListUpdated) {
            emit(AaveMarketRead(market = null, source = StatusSource.CACHE))
            updateShared(asset, countBeforeCheck)
        } else {
            StatusSource.ACTUAL
        }
        emitAll(observe(asset).map { AaveMarketRead(market = it, source = source) })
    }

    fun observeAll(): Flow<List<YieldMarketToken>> = yieldSupplyRepository.getMarketsFlow()

    suspend fun update(): Either<Throwable, Unit> = updateMutex.withLock { fetch() }

    private fun observe(asset: LendingAsset): Flow<YieldMarketToken?> = observeAll().map { it.findFor(asset) }

    private suspend fun current(asset: LendingAsset): YieldMarketToken? = observe(asset).first()

    private suspend fun updateShared(asset: LendingAsset, countBeforeCheck: Int): StatusSource = updateMutex.withLock {
        when {
            updateCount != countBeforeCheck -> lastUpdateSource
            isListUpdated || current(asset) != null -> StatusSource.ACTUAL
            else -> {
                fetch()
                lastUpdateSource
            }
        }
    }

    private suspend fun fetch(): Either<Throwable, Unit> {
        val result = Either.catch { yieldSupplyRepository.updateMarkets() }
            .map { }
            .onLeft { TangemLogger.e("Aave markets update failed", it) }
        if (result.isRight()) isListUpdated = true
        lastUpdateSource = if (result.isRight()) StatusSource.ACTUAL else StatusSource.ONLY_CACHE
        updateCount++
        return result
    }
}

internal data class AaveMarketRead(val market: YieldMarketToken?, val source: StatusSource)

internal fun List<YieldMarketToken>.findFor(asset: LendingAsset): YieldMarketToken? {
    val key = asset.reserveToken.yieldSupplyKey()
    return firstOrNull { it.yieldSupplyKey == key }
}