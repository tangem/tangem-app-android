package com.tangem.data.common.network

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkFactoryTest {

    private val networkFactory = NetworkFactory(excludedBlockchains = ExcludedBlockchains())

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        @Test
        fun `create if blockchain is excluded`() {
            // Arrange
            val excludedBlockchains = mockk<ExcludedBlockchains>()
            val networkFactory = NetworkFactory(excludedBlockchains = excludedBlockchains)

            every { any() in excludedBlockchains } returns true

            // Act
            val actual = networkFactory.create(
                blockchain = Blockchain.Ethereum,
                extraDerivationPath = null,
                userWallet = createUserWallet(),
            )

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun create(model: CreateTestModel) {
            // Act
            val actual = when (model) {
                is CreateTestModel.First -> {
                    networkFactory.create(
                        blockchain = model.blockchain,
                        extraDerivationPath = model.extraDerivationPath,
                        userWallet = model.userWallet,
                    )
                }
                is CreateTestModel.Second -> {
                    networkFactory.create(
                        networkId = model.networkId,
                        derivationPath = model.derivationPath,
                        userWallet = model.userWallet,
                    )
                }
                is CreateTestModel.Third -> {
                    networkFactory.create(
                        blockchain = model.blockchain,
                        extraDerivationPath = model.extraDerivationPath,
                        derivationStyleProvider = model.derivationStyleProvider,
                        canHandleTokens = model.canHandleTokens,
                    )
                }
            }

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            // region create(blockchain: Blockchain, extraDerivationPath: String?, scanResponse: ScanResponse)
            CreateTestModel.First(
                blockchain = Blockchain.Unknown,
                extraDerivationPath = null, // never-mind
                userWallet = createUserWallet(),
                expected = null,
            ),
            createFirst(
                extraDerivationPath = null,
                userWallet = createUserWallet(createGenericScanResponse()), // default derivation path is null
                expectedDerivationPath = Network.DerivationPath.None,
            ),
            createFirst(
                extraDerivationPath = null,
                userWallet = createUserWallet(),
                expectedDerivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0"), // use default
            ),
            createFirst(
                extraDerivationPath = "m/44'/60'/0'/0/0", // as default
                userWallet = createUserWallet(),
                expectedDerivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0"),
            ),
            createFirst(
                extraDerivationPath = "m/84'/0'/0'/0/0",
                userWallet = createUserWallet(),
                expectedDerivationPath = Network.DerivationPath.Custom(value = "m/84'/0'/0'/0/0"),
            ),
            // endregion

            // region create(networkId: Network.ID, derivationPath: Network.DerivationPath, scanResponse: ScanResponse)
            CreateTestModel.Second(
                networkId = Network.ID(value = "1", derivationPath = Network.DerivationPath.None),
                derivationPath = Network.DerivationPath.None, // never-mind
                userWallet = createUserWallet(), // never-mind
                expected = null,
            ),
            createSecond(
                userWallet = createUserWallet(createGenericScanResponse()), // default derivation path is null
                derivationPath = Network.DerivationPath.None,
            ),
            createSecond(
                userWallet = createUserWallet(),
                derivationPath = Network.DerivationPath.None,
            ),
            createSecond(
                userWallet = createUserWallet(),
                derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0"),
            ),
            createSecond(
                userWallet = createUserWallet(),
                derivationPath = Network.DerivationPath.Custom(value = "m/84'/0'/0'/0/0"),
            ),
            // endregion

            // region fun create(blockchain: Blockchain, extraDerivationPath: String?, derivationStyleProvider: DerivationStyleProvider?, canHandleTokens: Boolean)
            CreateTestModel.Third(
                blockchain = Blockchain.Unknown,
                extraDerivationPath = null, // never-mind
                derivationStyleProvider = createMultiWalletScanResponse().derivationStyleProvider, // never-mind
                canHandleTokens = true, // never-mind
                expected = null,
            ),
            createThird(
                extraDerivationPath = null,
                derivationStyleProvider = createGenericScanResponse().derivationStyleProvider, // default derivation path is null
                canHandleTokens = true,
                expectedDerivationPath = Network.DerivationPath.None,
            ),
            createThird(
                extraDerivationPath = null,
                derivationStyleProvider = createMultiWalletScanResponse().derivationStyleProvider,
                canHandleTokens = true,
                expectedDerivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0"), // use default
            ),
            createThird(
                extraDerivationPath = "m/44'/60'/0'/0/0", // as default
                derivationStyleProvider = createMultiWalletScanResponse().derivationStyleProvider,
                canHandleTokens = false,
                expectedDerivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0"),
            ),
            createThird(
                extraDerivationPath = "m/84'/0'/0'/0/0",
                derivationStyleProvider = createMultiWalletScanResponse().derivationStyleProvider,
                canHandleTokens = false,
                expectedDerivationPath = Network.DerivationPath.Custom(value = "m/84'/0'/0'/0/0"),
            ),
            // endregion
        )

        private fun createFirst(
            extraDerivationPath: String?,
            userWallet: UserWallet,
            expectedDerivationPath: Network.DerivationPath,
        ): CreateTestModel.First {
            return CreateTestModel.First(
                blockchain = Blockchain.Ethereum,
                extraDerivationPath = extraDerivationPath,
                userWallet = userWallet,
                expected = MockCryptoCurrencyFactory().ethereum.network.copy(
                    id = Network.ID(
                        value = Blockchain.Ethereum.id,
                        derivationPath = expectedDerivationPath,
                    ),
                    derivationPath = expectedDerivationPath,
                    canHandleTokens = true,
                ),
            )
        }

        private fun createSecond(
            userWallet: UserWallet,
            derivationPath: Network.DerivationPath,
        ): CreateTestModel.Second {
            return CreateTestModel.Second(
                networkId = Network.ID(value = Blockchain.Ethereum.id, derivationPath = derivationPath),
                derivationPath = derivationPath,
                userWallet = userWallet,
                expected = MockCryptoCurrencyFactory().ethereum.network.copy(
                    id = Network.ID(
                        value = Blockchain.Ethereum.id,
                        derivationPath = derivationPath,
                    ),
                    derivationPath = derivationPath,
                    canHandleTokens = true,
                ),
            )
        }

        private fun createThird(
            extraDerivationPath: String?,
            derivationStyleProvider: DerivationStyleProvider?,
            canHandleTokens: Boolean,
            expectedDerivationPath: Network.DerivationPath,
        ): CreateTestModel.Third {
            return CreateTestModel.Third(
                blockchain = Blockchain.Ethereum,
                extraDerivationPath = extraDerivationPath,
                derivationStyleProvider = derivationStyleProvider,
                canHandleTokens = canHandleTokens,
                expected = MockCryptoCurrencyFactory().ethereum.network.copy(
                    id = Network.ID(value = Blockchain.Ethereum.id, derivationPath = expectedDerivationPath),
                    derivationPath = expectedDerivationPath,
                    canHandleTokens = canHandleTokens,
                ),
            )
        }

        private fun createUserWallet(scanResponse: ScanResponse = createMultiWalletScanResponse()): UserWallet.Cold {
            return MockUserWalletFactory.create(scanResponse)
        }

        private fun createMultiWalletScanResponse(): ScanResponse {
            return MockScanResponseFactory.create(
                cardConfig = MultiWalletCardConfig,
                derivedKeys = emptyMap(),
            )
        }

        private fun createGenericScanResponse(): ScanResponse {
            return MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 1),
                derivedKeys = emptyMap(),
            )
        }
    }

    sealed interface CreateTestModel {

        val expected: Network?

        data class First(
            val blockchain: Blockchain,
            val extraDerivationPath: String?,
            val userWallet: UserWallet,
            override val expected: Network?,
        ) : CreateTestModel

        data class Second(
            val networkId: Network.ID,
            val derivationPath: Network.DerivationPath,
            val userWallet: UserWallet,
            override val expected: Network?,
        ) : CreateTestModel

        data class Third(
            val blockchain: Blockchain,
            val extraDerivationPath: String?,
            val derivationStyleProvider: DerivationStyleProvider?,
            val canHandleTokens: Boolean,
            override val expected: Network?,
        ) : CreateTestModel
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetNetworkStandardType {

        @ParameterizedTest
        @ProvideTestModels
        fun getNetworkStandardType(model: GetNetworkStandardTypeModel) {
            // Act
            val actual = model.blockchains.map(networkFactory::createNetworkStandardType)

            // Assert
            val expected = model.blockchains.map(model.expected)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            GetNetworkStandardTypeModel(
                blockchains = listOf(Blockchain.Ethereum, Blockchain.EthereumTestnet),
                expected = { Network.StandardType.ERC20 },
            ),
            GetNetworkStandardTypeModel(
                blockchains = listOf(Blockchain.BSC, Blockchain.BSCTestnet),
                expected = { Network.StandardType.BEP20 },
            ),
            GetNetworkStandardTypeModel(
                blockchains = listOf(Blockchain.Binance, Blockchain.BinanceTestnet),
                expected = { Network.StandardType.BEP2 },
            ),
            GetNetworkStandardTypeModel(
                blockchains = listOf(Blockchain.Tron, Blockchain.TronTestnet),
                expected = { Network.StandardType.TRC20 },
            ),
            GetNetworkStandardTypeModel(
                blockchains = Blockchain.entries - listOf(
                    Blockchain.Ethereum,
                    Blockchain.EthereumTestnet,
                    Blockchain.BSC,
                    Blockchain.BSCTestnet,
                    Blockchain.Binance,
                    Blockchain.BinanceTestnet,
                    Blockchain.Tron,
                    Blockchain.TronTestnet,
                ),
                expected = { Network.StandardType.Unspecified(it.name) },
            ),
        )
    }

    data class GetNetworkStandardTypeModel(
        val blockchains: List<Blockchain>,
        val expected: (Blockchain) -> Network.StandardType,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetSupportedTransactionExtras {

        @ParameterizedTest
        @ProvideTestModels
        fun getSupportedTransactionExtras(model: GetSupportedTransactionExtrasModel) {
            // Act
            val actual = model.blockchains.map(networkFactory::createSupportedTransactionExtras)

            // Assert
            val expected = List(size = model.blockchains.size) { model.expected }

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            GetSupportedTransactionExtrasModel(
                blockchains = listOf(Blockchain.XRP),
                expected = Network.TransactionExtrasType.DESTINATION_TAG,
            ),
            GetSupportedTransactionExtrasModel(
                blockchains = listOf(
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
                ),
                expected = Network.TransactionExtrasType.MEMO,
            ),
            GetSupportedTransactionExtrasModel(
                blockchains = listOf(
                    Blockchain.Unknown,
                    Blockchain.Alephium, Blockchain.AlephiumTestnet,
                    Blockchain.Arbitrum, Blockchain.ArbitrumTestnet,
                    Blockchain.Avalanche, Blockchain.AvalancheTestnet,
                    Blockchain.BinanceTestnet,
                    Blockchain.BSC, Blockchain.BSCTestnet,
                    Blockchain.Bitcoin, Blockchain.BitcoinTestnet,
                    Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet,
                    Blockchain.Cardano,
                    Blockchain.CosmosTestnet,
                    Blockchain.Dogecoin,
                    Blockchain.Ducatus,
                    Blockchain.Ethereum, Blockchain.EthereumTestnet,
                    Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet,
                    Blockchain.Fantom, Blockchain.FantomTestnet,
                    Blockchain.Litecoin,
                    Blockchain.Near, Blockchain.NearTestnet,
                    Blockchain.Polkadot, Blockchain.PolkadotTestnet,
                    Blockchain.Kava, Blockchain.KavaTestnet,
                    Blockchain.Kusama,
                    Blockchain.Polygon, Blockchain.PolygonTestnet,
                    Blockchain.RSK,
                    Blockchain.SeiTestnet,
                    Blockchain.StellarTestnet,
                    Blockchain.Solana, Blockchain.SolanaTestnet,
                    Blockchain.Tezos,
                    Blockchain.Tron, Blockchain.TronTestnet,
                    Blockchain.Gnosis,
                    Blockchain.Dash,
                    Blockchain.Optimism, Blockchain.OptimismTestnet,
                    Blockchain.Dischain,
                    Blockchain.EthereumPow, Blockchain.EthereumPowTestnet,
                    Blockchain.Kaspa, Blockchain.KaspaTestnet,
                    Blockchain.Telos, Blockchain.TelosTestnet,
                    Blockchain.TONTestnet,
                    Blockchain.Ravencoin,
                    Blockchain.Clore,
                    Blockchain.RavencoinTestnet,
                    Blockchain.Cronos,
                    Blockchain.AlephZero, Blockchain.AlephZeroTestnet,
                    Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet,
                    Blockchain.Chia, Blockchain.ChiaTestnet,
                    Blockchain.Decimal, Blockchain.DecimalTestnet,
                    Blockchain.XDC, Blockchain.XDCTestnet,
                    Blockchain.VeChain, Blockchain.VeChainTestnet,
                    Blockchain.Aptos, Blockchain.AptosTestnet,
                    Blockchain.Playa3ull,
                    Blockchain.Shibarium, Blockchain.ShibariumTestnet,
                    Blockchain.AlgorandTestnet,
                    Blockchain.HederaTestnet,
                    Blockchain.Aurora, Blockchain.AuroraTestnet,
                    Blockchain.Areon, Blockchain.AreonTestnet,
                    Blockchain.PulseChain, Blockchain.PulseChainTestnet,
                    Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet,
                    Blockchain.Nexa, Blockchain.NexaTestnet,
                    Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
                    Blockchain.Manta, Blockchain.MantaTestnet,
                    Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet,
                    Blockchain.Radiant,
                    Blockchain.Fact0rn,
                    Blockchain.Base, Blockchain.BaseTestnet,
                    Blockchain.Moonriver, Blockchain.MoonriverTestnet,
                    Blockchain.Mantle, Blockchain.MantleTestnet,
                    Blockchain.Flare, Blockchain.FlareTestnet,
                    Blockchain.Taraxa, Blockchain.TaraxaTestnet,
                    Blockchain.Koinos, Blockchain.KoinosTestnet,
                    Blockchain.Joystream,
                    Blockchain.Bittensor,
                    Blockchain.Filecoin,
                    Blockchain.Blast, Blockchain.BlastTestnet,
                    Blockchain.Cyber, Blockchain.CyberTestnet,
                    Blockchain.Sui, Blockchain.SuiTestnet,
                    Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
                    Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
                    Blockchain.CasperTestnet,
                    Blockchain.Core, Blockchain.CoreTestnet,
                    Blockchain.Xodex,
                    Blockchain.Canxium,
                    Blockchain.Chiliz, Blockchain.ChilizTestnet,
                    Blockchain.VanarChain, Blockchain.VanarChainTestnet,
                    Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet,
                    Blockchain.Bitrock, Blockchain.BitrockTestnet,
                    Blockchain.Sonic, Blockchain.SonicTestnet,
                    Blockchain.ApeChain, Blockchain.ApeChainTestnet,
                    Blockchain.Scroll, Blockchain.ScrollTestnet,
                    Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet,
                    Blockchain.Pepecoin, Blockchain.PepecoinTestnet,
                ),
                expected = Network.TransactionExtrasType.NONE,
            ),
        )
    }

    data class GetSupportedTransactionExtrasModel(
        val blockchains: List<Blockchain>,
        val expected: Network.TransactionExtrasType,
    )
}