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
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingYieldsStore: StakingYieldsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingRepository {

    private val yieldConverter = YieldConverter()

    override fun isStakingSupported(currencyId: String): Boolean {
        return integrationIds.contains(currencyId)
    }

    override suspend fun fetchEnabledYields() {
        withContext(dispatchers.io) {
            val stakingTokensWithYields = stakeKitApi.getMultipleYields().getOrThrow()

            stakingYieldsStore.store(stakingTokensWithYields.data)
        }
    }

    override suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield {
        return withContext(dispatchers.io) {
            val yields = getEnabledYields() ?: error("No yields found")
            val rawCurrencyId = cryptoCurrencyId.rawCurrencyId ?: error("Staking custom tokens is not available")

            val prefetchedYield = findPrefetchedYield(
                yields = yields,
                currencyId = rawCurrencyId,
                symbol = symbol,
            )

            prefetchedYield ?: error("Staking is unavailable")
        }
    }

    override suspend fun getEntryInfo(integrationId: String): StakingEntryInfo {
        return withContext(dispatchers.io) {
            val yield = stakeKitApi.getSingleYield(integrationId).getOrThrow()

            StakingEntryInfo(
                interestRate = yield.apy,
                periodInDays = yield.metadata.cooldownPeriod.days,
                tokenSymbol = yield.token.symbol,
            )
        }
    }

    override suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability {
        val rawCurrencyId = cryptoCurrencyId.rawCurrencyId ?: return StakingAvailability.Unavailable

        return withContext(dispatchers.io) {
            val yields = getEnabledYields() ?: return@withContext StakingAvailability.Unavailable

            val prefetchedYield = findPrefetchedYield(yields, rawCurrencyId, symbol)
            val isSupported = isStakingSupported(rawCurrencyId)

            when {
                prefetchedYield != null && isSupported -> {
                    StakingAvailability.Available(prefetchedYield.id)
                }
                prefetchedYield == null && isSupported -> {
                    StakingAvailability.TemporaryDisabled
                }
                else -> StakingAvailability.Unavailable
            }
        }
    }

    private fun findPrefetchedYield(yields: List<Yield>, currencyId: String, symbol: String): Yield? {
        return yields
            .find { it.token.coinGeckoId == currencyId && it.token.symbol == symbol }
    }

    private suspend fun getEnabledYields(): List<Yield>? {
        val yields = stakingYieldsStore.getSyncOrNull() ?: return null
        return yields.map { yieldConverter.convert(it) }
    }

    companion object {
        private val integrationIds = setOf(
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