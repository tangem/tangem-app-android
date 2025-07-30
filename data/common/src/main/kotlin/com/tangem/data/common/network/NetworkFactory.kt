package com.tangem.data.common.network

import androidx.annotation.VisibleForTesting
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.card.common.extensions.canHandleToken
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Factory for creating [Network]
 *
 * @property excludedBlockchains excluded blockchains
 *
[REDACTED_AUTHOR]
 */
class NetworkFactory @Inject constructor(
    private val excludedBlockchains: ExcludedBlockchains,
) {

    /**
     * Create
     *
     * @param blockchain          blockchain
     * @param extraDerivationPath extra derivation path
     * @param userWallet          user wallet
     */
    fun create(blockchain: Blockchain, extraDerivationPath: String?, userWallet: UserWallet): Network? {
        return create(
            blockchain = blockchain,
            derivationPath = createDerivationPath(
                blockchain = blockchain,
                extraDerivationPath = extraDerivationPath,
                cardDerivationStyleProvider = userWallet.derivationStyleProvider,
            ),
            canHandleTokens = userWallet.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            ),
        )
    }

    /**
     * Create
     *
     * @param networkId      network id
     * @param derivationPath derivation path
     * @param userWallet     user wallet
     */
    fun create(networkId: Network.ID, derivationPath: Network.DerivationPath, userWallet: UserWallet): Network? {
        val blockchain = networkId.toBlockchain()

        return create(
            blockchain = blockchain,
            derivationPath = derivationPath,
            canHandleTokens = userWallet.canHandleToken(
                blockchain = blockchain,
                excludedBlockchains = excludedBlockchains,
            ),
        )
    }

    /**
     * Create
     *
     * @param blockchain              blockchain
     * @param extraDerivationPath     extra derivation path
     * @param derivationStyleProvider derivation style provider
     * @param canHandleTokens         flag that indicates whether the network can handle tokens
     */
    fun create(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        derivationStyleProvider: DerivationStyleProvider?,
        canHandleTokens: Boolean,
    ): Network? {
        return create(
            blockchain = blockchain,
            derivationPath = createDerivationPath(
                blockchain = blockchain,
                extraDerivationPath = extraDerivationPath,
                cardDerivationStyleProvider = derivationStyleProvider,
            ),
            canHandleTokens = canHandleTokens,
        )
    }

    private fun create(
        blockchain: Blockchain,
        derivationPath: Network.DerivationPath,
        canHandleTokens: Boolean,
    ): Network? {
        if (!blockchain.isBlockchainSupported()) return null

        return runCatching {
            Network(
                id = Network.ID(value = blockchain.id, derivationPath = derivationPath),
                backendId = blockchain.toNetworkId(),
                name = blockchain.fullName,
                isTestnet = blockchain.isTestnet(),
                derivationPath = derivationPath,
                currencySymbol = blockchain.currency,
                standardType = getNetworkStandardType(blockchain),
                hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
                canHandleTokens = canHandleTokens,
                transactionExtrasType = blockchain.getSupportedTransactionExtras(),
                nameResolvingType = blockchain.getNameResolvingType(),
            )
        }
            .getOrNull()
    }

    private fun Blockchain.isBlockchainSupported(): Boolean {
        if (this == Blockchain.Unknown) {
            Timber.w("Unable to convert Unknown blockchain to the domain network model")
            return false
        }
        if (this in excludedBlockchains) {
            Timber.w("Unable to convert excluded blockchain to the domain network model")
            return false
        }

        return true
    }

    private fun createDerivationPath(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        cardDerivationStyleProvider: DerivationStyleProvider?,
    ): Network.DerivationPath {
        if (cardDerivationStyleProvider == null) return Network.DerivationPath.None

        val defaultDerivationPath = getDefaultDerivationPath(blockchain, cardDerivationStyleProvider)

        return if (extraDerivationPath.isNullOrBlank()) {
            if (defaultDerivationPath.isNullOrBlank()) {
                Network.DerivationPath.None
            } else {
                Network.DerivationPath.Card(defaultDerivationPath)
            }
        } else {
            if (extraDerivationPath == defaultDerivationPath) {
                Network.DerivationPath.Card(defaultDerivationPath)
            } else {
                Network.DerivationPath.Custom(extraDerivationPath)
            }
        }
    }

    private fun getDefaultDerivationPath(
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
    ): String? {
        return blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath
    }

    private fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.StandardType.ERC20
            Blockchain.BSC, Blockchain.BSCTestnet -> Network.StandardType.BEP20
            Blockchain.Binance, Blockchain.BinanceTestnet -> Network.StandardType.BEP2
            Blockchain.Tron, Blockchain.TronTestnet -> Network.StandardType.TRC20
            else -> Network.StandardType.Unspecified(blockchain.name)
        }
    }

    @Suppress("LongMethod")
    private fun Blockchain.getSupportedTransactionExtras(): Network.TransactionExtrasType {
        return when (this) {
            Blockchain.XRP -> Network.TransactionExtrasType.DESTINATION_TAG
            Blockchain.Binance,
            Blockchain.TON,
            Blockchain.Cosmos,
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            Blockchain.Stellar,
            Blockchain.Hedera,
            Blockchain.Algorand,
            Blockchain.Sei,
            Blockchain.InternetComputer,
            Blockchain.Casper,
            -> Network.TransactionExtrasType.MEMO
            // region Other blockchains
            Blockchain.Unknown,
            Blockchain.Alephium,
            Blockchain.AlephiumTestnet,
            Blockchain.Arbitrum,
            Blockchain.ArbitrumTestnet,
            Blockchain.Avalanche,
            Blockchain.AvalancheTestnet,
            Blockchain.BinanceTestnet,
            Blockchain.BSC,
            Blockchain.BSCTestnet,
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.BitcoinCash,
            Blockchain.BitcoinCashTestnet,
            Blockchain.Cardano,
            Blockchain.CosmosTestnet,
            Blockchain.Dogecoin,
            Blockchain.Ducatus,
            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassic,
            Blockchain.EthereumClassicTestnet,
            Blockchain.Fantom,
            Blockchain.FantomTestnet,
            Blockchain.Litecoin,
            Blockchain.Near,
            Blockchain.NearTestnet,
            Blockchain.Polkadot,
            Blockchain.PolkadotTestnet,
            Blockchain.Kava,
            Blockchain.KavaTestnet,
            Blockchain.Kusama,
            Blockchain.Polygon,
            Blockchain.PolygonTestnet,
            Blockchain.RSK,
            Blockchain.SeiTestnet,
            Blockchain.StellarTestnet,
            Blockchain.Solana,
            Blockchain.SolanaTestnet,
            Blockchain.Tezos,
            Blockchain.Tron,
            Blockchain.TronTestnet,
            Blockchain.Gnosis,
            Blockchain.Dash,
            Blockchain.Optimism,
            Blockchain.OptimismTestnet,
            Blockchain.Dischain,
            Blockchain.EthereumPow,
            Blockchain.EthereumPowTestnet,
            Blockchain.Kaspa,
            Blockchain.KaspaTestnet,
            Blockchain.Telos,
            Blockchain.TelosTestnet,
            Blockchain.TONTestnet,
            Blockchain.Ravencoin,
            Blockchain.Clore,
            Blockchain.RavencoinTestnet,
            Blockchain.Cronos,
            Blockchain.AlephZero,
            Blockchain.AlephZeroTestnet,
            Blockchain.OctaSpace,
            Blockchain.OctaSpaceTestnet,
            Blockchain.Chia,
            Blockchain.ChiaTestnet,
            Blockchain.Decimal,
            Blockchain.DecimalTestnet,
            Blockchain.XDC,
            Blockchain.XDCTestnet,
            Blockchain.VeChain,
            Blockchain.VeChainTestnet,
            Blockchain.Aptos,
            Blockchain.AptosTestnet,
            Blockchain.Playa3ull,
            Blockchain.Shibarium,
            Blockchain.ShibariumTestnet,
            Blockchain.AlgorandTestnet,
            Blockchain.HederaTestnet,
            Blockchain.Aurora,
            Blockchain.AuroraTestnet,
            Blockchain.Areon,
            Blockchain.AreonTestnet,
            Blockchain.PulseChain,
            Blockchain.PulseChainTestnet,
            Blockchain.ZkSyncEra,
            Blockchain.ZkSyncEraTestnet,
            Blockchain.Nexa,
            Blockchain.NexaTestnet,
            Blockchain.Moonbeam,
            Blockchain.MoonbeamTestnet,
            Blockchain.Manta,
            Blockchain.MantaTestnet,
            Blockchain.PolygonZkEVM,
            Blockchain.PolygonZkEVMTestnet,
            Blockchain.Radiant,
            Blockchain.Fact0rn,
            Blockchain.Base,
            Blockchain.BaseTestnet,
            Blockchain.Moonriver,
            Blockchain.MoonriverTestnet,
            Blockchain.Mantle,
            Blockchain.MantleTestnet,
            Blockchain.Flare,
            Blockchain.FlareTestnet,
            Blockchain.Taraxa,
            Blockchain.TaraxaTestnet,
            Blockchain.Koinos,
            Blockchain.KoinosTestnet,
            Blockchain.Joystream,
            Blockchain.Bittensor,
            Blockchain.Filecoin,
            Blockchain.Blast,
            Blockchain.BlastTestnet,
            Blockchain.Cyber,
            Blockchain.CyberTestnet,
            Blockchain.Sui,
            Blockchain.SuiTestnet,
            Blockchain.EnergyWebChain,
            Blockchain.EnergyWebChainTestnet,
            Blockchain.EnergyWebX,
            Blockchain.EnergyWebXTestnet,
            Blockchain.CasperTestnet,
            Blockchain.Core,
            Blockchain.CoreTestnet,
            Blockchain.Xodex,
            Blockchain.Canxium,
            Blockchain.Chiliz,
            Blockchain.ChilizTestnet,
            Blockchain.VanarChain,
            Blockchain.VanarChainTestnet,
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet,
            Blockchain.Bitrock, Blockchain.BitrockTestnet,
            Blockchain.Sonic, Blockchain.SonicTestnet,
            Blockchain.ApeChain, Blockchain.ApeChainTestnet,
            Blockchain.Scroll, Blockchain.ScrollTestnet,
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet,
            Blockchain.Pepecoin, Blockchain.PepecoinTestnet,
            -> Network.TransactionExtrasType.NONE
            // endregion
        }
    }

    private fun Blockchain.getNameResolvingType(): Network.NameResolvingType {
        return when (this) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.NameResolvingType.ENS
            else -> Network.NameResolvingType.NONE
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun createNetworkStandardType(blockchain: Blockchain) = getNetworkStandardType(blockchain)

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun createSupportedTransactionExtras(blockchain: Blockchain) = blockchain.getSupportedTransactionExtras()
}