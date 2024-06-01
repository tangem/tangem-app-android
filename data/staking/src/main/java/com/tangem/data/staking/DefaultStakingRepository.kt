package com.tangem.data.staking

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.staking.converters.YieldConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val stakingYieldsStore: StakingYieldsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingRepository {

    private val yieldConverter = YieldConverter()

    override suspend fun getStakingAvailabilityForActions(currencyId: String, symbol: String): StakingAvailability {
        if (!stakingFeatureToggles.isStakingEnabled) {
            return StakingAvailability.Unavailable
        }

        val yields = getEnabledYields() ?: return StakingAvailability.Unavailable

        val prefetchedYield = findPrefetchedYield(yields, currencyId, symbol)
        val isSupported = isStakingSupported(currencyId)

        return when {
            prefetchedYield != null && isSupported -> {
                StakingAvailability.Available(prefetchedYield.id)
            }
            prefetchedYield == null && isSupported -> {
                StakingAvailability.TemporaryDisabled
            }
            else -> StakingAvailability.Unavailable
        }
    }

    private fun findPrefetchedYield(yields: List<Yield>, currencyId: String, symbol: String): Yield? {
        return yields
            .find { it.token.coinGeckoId == currencyId && it.token.symbol == symbol }
    }

    override fun isStakingSupported(currencyId: String): Boolean {
        return integrationIdMap.contains(currencyId)
    }

    override suspend fun getEntryInfo(integrationId: String): StakingEntryInfo {
        val yield = stakeKitApi.getSingleYield(integrationId).getOrThrow()

        return StakingEntryInfo(
            interestRate = yield.apy,
            periodInDays = yield.metadata.cooldownPeriod.days,
            tokenSymbol = yield.token.symbol,
        )
    }

    override suspend fun fetchEnabledYields() {
        if (!stakingFeatureToggles.isStakingEnabled) {
            return
        }

        withContext(dispatchers.io) {
            val stakingTokensWithYields = stakeKitApi.getMultipleYields().getOrThrow()

            stakingYieldsStore.store(stakingTokensWithYields.data)
        }
    }

    private suspend fun getEnabledYields(): List<Yield>? {
        val yields = stakingYieldsStore.getSyncOrNull() ?: return null
        return yields.map { yieldConverter.convert(it) }
    }

    companion object {
        private val integrationIdMap = setOf(
            Blockchain.Solana.toCoinId(),
            Blockchain.Cosmos.toCoinId(),
            Blockchain.Polkadot.toCoinId(),
            Blockchain.Polygon.toCoinId(),
            Blockchain.Avalanche.toCoinId(),
            Blockchain.Tron.toCoinId(),
            Blockchain.Cronos.toCoinId(),
            Blockchain.Binance.toCoinId(),
            Blockchain.Kava.toCoinId(),
            Blockchain.Near.toCoinId(),
            Blockchain.Tezos.toCoinId(),
        )
    }
}
