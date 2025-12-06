package com.tangem.lib.crypto.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.isUTXO
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.network.Network
import timber.log.Timber

/**
 * Utility class to recognize the account node in a derivation path based on the blockchain type.
 * Derivation path schema: [ m / purpose' / coin_type' / account' / change / address_index ].
 *
 * @property blockchain the blockchain for which the account node is to be recognized
 *
 * @see [iOS](https://github.com/tangem-developments/tangem-app-ios/blob/f5312a8177afbebda2ff2ed93771f11fae4837bb/Tangem/Domain/Accounts/Common/AccountDerivationPathHelper.swift)
[REDACTED_AUTHOR]
 */
class AccountNodeRecognizer(private val blockchain: Blockchain) {

    /**
     * Index of the account node in the [derivationPath]
     */
    @Suppress("MagicNumber")
    fun getAccountNodeIndex(derivationPath: DerivationPath): Int? {
        val nodesCount = derivationPath.nodes.size

        val index = when {
            blockchain == Blockchain.Tezos -> {
                UTXO_BLOCKCHAIN_NODE_INDEX.takeIf { nodesCount == 4 }
            }
            blockchain == Blockchain.Quai || blockchain.isUTXO -> {
                UTXO_BLOCKCHAIN_NODE_INDEX.takeIf { nodesCount == 5 }
            }
            !blockchain.isUTXO -> {
                (nodesCount - 1).takeIf { nodesCount == 3 || nodesCount == 5 }
            }
            else -> null
        }

        if (index == null) {
            Timber.e("Cannot determine account node index for ${blockchain.fullName}: ${derivationPath.rawPath}")
        }

        return index
    }

    /** Recognizes the account node value from the given [derivationPath] */
    fun recognize(derivationPath: Network.DerivationPath): Long? {
        val derivationPathValue = derivationPath.value ?: return null

        return recognize(derivationPathValue = derivationPathValue)
    }

    /** Recognizes the account node value from the given derivation path string [derivationPathValue] */
    fun recognize(derivationPathValue: String): Long? {
        if (derivationPathValue.isBlank()) return null

        return runCatching {
            val cardSdkDerivationPath = DerivationPath(rawPath = derivationPathValue)
            recognize(derivationPath = cardSdkDerivationPath)
        }.getOrNull()
    }

    /** Recognizes the account node value from the given [derivationPath] */
    fun recognize(derivationPath: DerivationPath): Long? {
        return runCatching {
            if (!blockchain.isAccountsSupported()) {
                Timber.e("Account derivation is not supported for blockchain: ${blockchain.fullName}")
                return null
            }

            val accountNodeIndex = getAccountNodeIndex(derivationPath) ?: return null
            val accountNode = derivationPath.nodes.getOrNull(accountNodeIndex)

            accountNode?.getIndex(includeHardened = false)
        }
            .getOrNull()
    }

    @Suppress("LongMethod")
    private fun Blockchain.isAccountsSupported(): Boolean {
        return when (this) {
            Blockchain.Bitcoin,
            Blockchain.Litecoin,
            Blockchain.Stellar,
            Blockchain.Ethereum,
            Blockchain.EthereumPow,
            Blockchain.Dischain,
            Blockchain.EthereumClassic,
            Blockchain.RSK,
            Blockchain.BitcoinCash,
            Blockchain.Binance,
            Blockchain.Cardano,
            Blockchain.XRP,
            Blockchain.Ducatus,
            Blockchain.Tezos,
            Blockchain.Dogecoin,
            Blockchain.BSC,
            Blockchain.Polygon,
            Blockchain.Avalanche,
            Blockchain.Solana,
            Blockchain.Fantom,
            Blockchain.Polkadot,
            Blockchain.Kusama,
            Blockchain.AlephZero,
            Blockchain.Tron,
            Blockchain.Arbitrum,
            Blockchain.Dash,
            Blockchain.Gnosis,
            Blockchain.Optimism,
            Blockchain.TON,
            Blockchain.Kava,
            Blockchain.Kaspa,
            Blockchain.Ravencoin,
            Blockchain.Cosmos,
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            Blockchain.Cronos,
            Blockchain.Telos,
            Blockchain.OctaSpace,
            Blockchain.Near,
            Blockchain.Decimal,
            Blockchain.VeChain,
            Blockchain.XDC,
            Blockchain.Algorand,
            Blockchain.Shibarium,
            Blockchain.Aptos,
            Blockchain.Hedera,
            Blockchain.Areon,
            Blockchain.Playa3ull,
            Blockchain.PulseChain,
            Blockchain.Aurora,
            Blockchain.Manta,
            Blockchain.ZkSyncEra,
            Blockchain.Moonbeam,
            Blockchain.PolygonZkEVM,
            Blockchain.Moonriver,
            Blockchain.Mantle,
            Blockchain.Flare,
            Blockchain.Taraxa,
            Blockchain.Radiant,
            Blockchain.Base,
            Blockchain.Joystream,
            Blockchain.Bittensor,
            Blockchain.Koinos,
            Blockchain.InternetComputer,
            Blockchain.Cyber,
            Blockchain.Blast,
            Blockchain.Sui,
            Blockchain.Filecoin,
            Blockchain.Sei,
            Blockchain.EnergyWebChain,
            Blockchain.EnergyWebX,
            Blockchain.Core,
            Blockchain.Canxium,
            Blockchain.Casper,
            Blockchain.Chiliz,
            Blockchain.Xodex,
            Blockchain.Clore,
            Blockchain.Fact0rn,
            Blockchain.OdysseyChain,
            Blockchain.Bitrock,
            Blockchain.ApeChain,
            Blockchain.Sonic,
            Blockchain.Alephium,
            Blockchain.VanarChain,
            Blockchain.ZkLinkNova,
            Blockchain.Pepecoin,
            Blockchain.Hyperliquid,
            Blockchain.Scroll,
            // Blockchain.Linea,
            // Blockchain.ArbitrumNova,
            Blockchain.Quai,
            Blockchain.Ink,
            -> true
            Blockchain.Nexa, // unsupported network
            Blockchain.Chia,
            -> false
            // region Testnet
            Blockchain.Unknown,
            Blockchain.ArbitrumTestnet,
            Blockchain.AvalancheTestnet,
            Blockchain.BinanceTestnet,
            Blockchain.BSCTestnet,
            Blockchain.BitcoinTestnet,
            Blockchain.BitcoinCashTestnet,
            Blockchain.CosmosTestnet,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassicTestnet,
            Blockchain.FantomTestnet,
            Blockchain.NearTestnet,
            Blockchain.PolkadotTestnet,
            Blockchain.KavaTestnet,
            Blockchain.PolygonTestnet,
            Blockchain.SeiTestnet,
            Blockchain.StellarTestnet,
            Blockchain.SolanaTestnet,
            Blockchain.TronTestnet,
            Blockchain.OptimismTestnet,
            Blockchain.EthereumPowTestnet,
            Blockchain.KaspaTestnet,
            Blockchain.TelosTestnet,
            Blockchain.TONTestnet,
            Blockchain.RavencoinTestnet,
            Blockchain.AlephZeroTestnet,
            Blockchain.OctaSpaceTestnet,
            Blockchain.ChiaTestnet,
            Blockchain.DecimalTestnet,
            Blockchain.XDCTestnet,
            Blockchain.VeChainTestnet,
            Blockchain.AptosTestnet,
            Blockchain.ShibariumTestnet,
            Blockchain.AlgorandTestnet,
            Blockchain.HederaTestnet,
            Blockchain.AuroraTestnet,
            Blockchain.AreonTestnet,
            Blockchain.PulseChainTestnet,
            Blockchain.ZkSyncEraTestnet,
            Blockchain.NexaTestnet,
            Blockchain.MoonbeamTestnet,
            Blockchain.MantaTestnet,
            Blockchain.PolygonZkEVMTestnet,
            Blockchain.BaseTestnet,
            Blockchain.MoonriverTestnet,
            Blockchain.MantleTestnet,
            Blockchain.FlareTestnet,
            Blockchain.TaraxaTestnet,
            Blockchain.KoinosTestnet,
            Blockchain.BlastTestnet,
            Blockchain.CyberTestnet,
            Blockchain.SuiTestnet,
            Blockchain.EnergyWebChainTestnet,
            Blockchain.EnergyWebXTestnet,
            Blockchain.CasperTestnet,
            Blockchain.CoreTestnet,
            Blockchain.ChilizTestnet,
            Blockchain.AlephiumTestnet,
            Blockchain.VanarChainTestnet,
            Blockchain.OdysseyChainTestnet,
            Blockchain.BitrockTestnet,
            Blockchain.SonicTestnet,
            Blockchain.ApeChainTestnet,
            Blockchain.ScrollTestnet,
            Blockchain.ZkLinkNovaTestnet,
            Blockchain.PepecoinTestnet,
            Blockchain.HyperliquidTestnet,
            Blockchain.QuaiTestnet,
            Blockchain.InkTestnet,
            // Blockchain.LineaTestnet,
            -> false
            // endregion
        }
    }

    private companion object {
        const val UTXO_BLOCKCHAIN_NODE_INDEX = 2
    }
}