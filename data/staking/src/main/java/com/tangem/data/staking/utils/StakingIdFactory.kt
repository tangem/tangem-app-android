package com.tangem.data.staking.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toMigratedCoinId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import javax.inject.Inject

/**
 * Factory of [StakingID]
 *
 * @property walletManagersFacade wallet manager facade
 *
[REDACTED_AUTHOR]
 */
internal class StakingIdFactory @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend fun create(userWalletId: UserWalletId, currencyId: CryptoCurrency.ID, network: Network): Set<StakingID> {
        val addresses = walletManagersFacade.getAddresses(userWalletId = userWalletId, network = network)

        val integrationId = createIntegrationId(currencyId) ?: return emptySet()

        return addresses.mapTo(hashSetOf()) { address ->
            StakingID(integrationId = integrationId, address = address.value)
        }
    }

    suspend fun createForDefault(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network,
    ): StakingID? {
        val address = walletManagersFacade.getDefaultAddress(userWalletId = userWalletId, network = network)
        val integrationId = createIntegrationId(currencyId)

        if (address == null || integrationId == null) return null

        return StakingID(integrationId = integrationId, address = address)
    }

    fun createIntegrationId(currencyId: CryptoCurrency.ID): String? {
        val integrationKey = with(currencyId) { rawNetworkId.plus(rawCurrencyId) }
        return integrationIdMap[integrationKey]
    }

    @Suppress("UnusedPrivateMember", "unused")
    companion object {

        private const val TON_INTEGRATION_ID = "ton-ton-chorus-one-pools-staking"
        private const val SOLANA_INTEGRATION_ID = "solana-sol-native-multivalidator-staking"
        private const val COSMOS_INTEGRATION_ID = "cosmos-atom-native-staking"
        private const val ETHEREUM_POLYGON_INTEGRATION_ID = "ethereum-matic-native-staking"
        private const val BINANCE_INTEGRATION_ID = "bsc-bnb-native-staking"
        private const val POLKADOT_INTEGRATION_ID = "polkadot-dot-validator-staking"
        private const val AVALANCHE_INTEGRATION_ID = "avalanche-avax-native-staking"
        private const val TRON_INTEGRATION_ID = "tron-trx-native-staking"
        private const val CRONOS_INTEGRATION_ID = "cronos-cro-native-staking"
        private const val KAVA_INTEGRATION_ID = "kava-kava-native-staking"
        private const val NEAR_INTEGRATION_ID = "near-near-native-staking"
        private const val TEZOS_INTEGRATION_ID = "tezos-xtz-native-staking"
        private const val CARDANO_INTEGRATION_ID = "cardano-ada-native-staking"

        // uncomment items as implementation is ready
        val integrationIdMap = mapOf(
            Blockchain.TON.toDefaultKey() to TON_INTEGRATION_ID,
            Blockchain.Solana.toDefaultKey() to SOLANA_INTEGRATION_ID,
            Blockchain.Cosmos.toDefaultKey() to COSMOS_INTEGRATION_ID,
            Blockchain.Tron.toDefaultKey() to TRON_INTEGRATION_ID,
            Blockchain.Ethereum.id + Blockchain.Polygon.toMigratedCoinId() to ETHEREUM_POLYGON_INTEGRATION_ID,
            // Blockchain.Ethereum.id + Blockchain.Polygon.toCoinId() to ETHEREUM_POLYGON_INTEGRATION_ID,
            Blockchain.BSC.toDefaultKey() to BINANCE_INTEGRATION_ID,
            // Blockchain.Polkadot.toDefaultKey() to POLKADOT_INTEGRATION_ID,
            // Blockchain.Avalanche.toDefaultKey() to AVALANCHE_INTEGRATION_ID,
            // Blockchain.Cronos.toDefaultKey() to CRONOS_INTEGRATION_ID,
            // Blockchain.Kava.toDefaultKey() to KAVA_INTEGRATION_ID,
            // Blockchain.Near.toDefaultKey() to NEAR_INTEGRATION_ID,
            // Blockchain.Tezos.toDefaultKey() to TEZOS_INTEGRATION_ID,
            Blockchain.Cardano.toDefaultKey() to CARDANO_INTEGRATION_ID,
        )

        private fun Blockchain.toDefaultKey(): String = id + toCoinId()
    }
}