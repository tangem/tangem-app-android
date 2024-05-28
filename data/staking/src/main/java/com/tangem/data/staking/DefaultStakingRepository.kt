package com.tangem.data.staking

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
    private val stakingFeatureToggles: StakingFeatureToggles
) : StakingRepository {

    override fun getStakingAvailability(blockchainId: String): StakingAvailability {
        if (!stakingFeatureToggles.isStakingEnabled) {
            return StakingAvailability.Unavailable
        }

        return when (Blockchain.fromId(blockchainId)) {
            Blockchain.Solana -> StakingAvailability.Available("solana-sol-native-multivalidator-staking")
            Blockchain.Cosmos -> StakingAvailability.Available("cosmos-atom-native-staking")
            Blockchain.Polkadot -> StakingAvailability.Available("polkadot-dot-validator-staking")
            Blockchain.Polygon -> StakingAvailability.Available("ethereum-matic-native-staking")
            Blockchain.Avalanche -> StakingAvailability.Available("avalanche-avax-native-staking")
            Blockchain.Tron -> StakingAvailability.Available("tron-trx-native-staking")
            Blockchain.Cronos -> StakingAvailability.Available("cronos-cro-native-staking")
            Blockchain.Binance -> StakingAvailability.Available("binance-bnb-native-staking")
            Blockchain.Kava -> StakingAvailability.Available("kava-kava-native-staking")
            Blockchain.Near -> StakingAvailability.Available("near-near-native-staking")
            Blockchain.Tezos -> StakingAvailability.Available("tezos-xtz-native-staking")
            else -> StakingAvailability.Unavailable
        }
    }

    override suspend fun getEntryInfo(integrationId: String): StakingEntryInfo {
        val yield = stakeKitApi.getSingleYield(integrationId).getOrThrow()

        return StakingEntryInfo(
            percent = yield.apy,
            periodInDays = yield.metadata.cooldownPeriod.days,
            tokenSymbol = yield.token.symbol,
        )
    }
}
