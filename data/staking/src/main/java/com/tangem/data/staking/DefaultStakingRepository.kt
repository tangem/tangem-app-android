package com.tangem.data.staking

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.staking.converters.*
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.Address
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.StakingBalanceStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingYieldsStore: StakingYieldsStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingRepository {

    private val stakingNetworkTypeConverter = StakingNetworkTypeConverter()

    private val tokenConverter = TokenConverter(
        stakingNetworkTypeConverter = stakingNetworkTypeConverter,
    )
    private val yieldConverter = YieldConverter(
        tokenConverter = tokenConverter,
    )

    private val yieldBalanceConverter = YieldBalanceConverter()

    private val yieldBalanceListConverter = YieldBalanceListConverter()

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

    override suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val result = stakeKitApi.getSingleYieldBalance(
                    integrationId = integrationId,
                    body = getBalanceRequestData(address, integrationId),
                ).getOrThrow()

                stakingBalanceStore.store(
                    integrationId,
                    YieldBalanceWrapperDTO(
                        balances = result,
                        integrationId = integrationId,
                    ),
                )
            },
        )
    }

    override fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): Flow<YieldBalance> = channelFlow {
        launch(dispatchers.io) {
            stakingBalanceStore.get(integrationId)
                .collectLatest {
                    send(
                        yieldBalanceConverter.convert(
                            YieldBalanceConverter.Data(
                                balance = it,
                                integrationId = integrationId,
                            ),
                        ),
                    )
                }
        }

        withContext(dispatchers.io) {
            fetchSingleYieldBalance(
                userWalletId,
                address,
                integrationId,
            )
        }
    }.cancellable()

    override suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        integrationId: String,
        refresh: Boolean,
    ) = withContext(dispatchers.io) {
        cacheRegistry.invokeOnExpire(
            key = getYieldBalancesKey(userWalletId),
            skipCache = refresh,
            block = {
                val result = stakeKitApi.getMultipleYieldBalances(
                    addresses.map { getBalanceRequestData(it.address, integrationId) },
                ).getOrThrow()

                stakingBalanceStore.store(result)
            },
        )
    }

    override fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        integrationId: String,
    ): Flow<YieldBalanceList> = channelFlow {
        launch(dispatchers.io) {
            stakingBalanceStore.get()
                .collectLatest { send(yieldBalanceListConverter.convert(it)) }
        }

        withContext(dispatchers.io) {
            fetchMultiYieldBalance(
                userWalletId,
                addresses,
                integrationId,
            )
        }
    }.cancellable()

    private fun findPrefetchedYield(yields: List<Yield>, currencyId: String, symbol: String): Yield? {
        return yields.find { it.token.coinGeckoId == currencyId && it.token.symbol == symbol }
    }

    private suspend fun getEnabledYields(): List<Yield>? {
        val yields = stakingYieldsStore.getSyncOrNull() ?: return null
        return yields.map { yieldConverter.convert(it) }
    }

    private fun getBalanceRequestData(address: String, integrationId: String): YieldBalanceRequestBody {
        return YieldBalanceRequestBody(
            addresses = Address(
                address = address,
                additionalAddresses = null, // todo fill additional addresses metadata if needed
                explorerUrl = "", // todo fill exporer url [REDACTED_JIRA]
            ),
            args = YieldBalanceRequestBody.YieldBalanceRequestArgs(
                validatorAddresses = listOf(), // todo add validators [REDACTED_JIRA]
            ),
            integrationId = integrationId,
        )
    }

    private fun getYieldBalancesKey(userWalletId: UserWalletId) = "yield_balance_${userWalletId.stringValue}"

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