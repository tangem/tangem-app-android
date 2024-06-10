package com.tangem.data.staking

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingRepository {

    override fun getStakingAvailability(blockchainId: String): StakingAvailability {
        if (!stakingFeatureToggles.isStakingEnabled) {
            return StakingAvailability.Unavailable
        }

        return integrationIdMap[Blockchain.fromId(blockchainId)]?.let {
            StakingAvailability.Available(it)
        } ?: StakingAvailability.Unavailable
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

    companion object {
        private const val SOLANA_INTEGRATION_ID = "solana-sol-native-multivalidator-staking"
        private const val COSMOS_INTEGRATION_ID = "cosmos-atom-native-staking"
        private const val POLKADOT_INTEGRATION_ID = "polkadot-dot-validator-staking"
        private const val ETHEREUM_INTEGRATION_ID = "ethereum-matic-native-staking"
        private const val AVALANCHE_INTEGRATION_ID = "avalanche-avax-native-staking"
        private const val TRON_INTEGRATION_ID = "tron-trx-native-staking"
        private const val CRONOS_INTEGRATION_ID = "cronos-cro-native-staking"
        private const val BINANCE_INTEGRATION_ID = "binance-bnb-native-staking"
        private const val KAVA_INTEGRATION_ID = "kava-kava-native-staking"
        private const val NEAR_INTEGRATION_ID = "near-near-native-staking"
        private const val TEZOS_INTEGRATION_ID = "tezos-xtz-native-staking"

        private val integrationIdMap = mapOf(
            Blockchain.Solana to SOLANA_INTEGRATION_ID,
            Blockchain.Cosmos to COSMOS_INTEGRATION_ID,
            Blockchain.Polkadot to POLKADOT_INTEGRATION_ID,
            Blockchain.Polygon to ETHEREUM_INTEGRATION_ID,
            Blockchain.Avalanche to AVALANCHE_INTEGRATION_ID,
            Blockchain.Tron to TRON_INTEGRATION_ID,
            Blockchain.Cronos to CRONOS_INTEGRATION_ID,
            Blockchain.Binance to BINANCE_INTEGRATION_ID,
            Blockchain.Kava to KAVA_INTEGRATION_ID,
            Blockchain.Near to NEAR_INTEGRATION_ID,
            Blockchain.Tezos to TEZOS_INTEGRATION_ID,
        )
    }
}