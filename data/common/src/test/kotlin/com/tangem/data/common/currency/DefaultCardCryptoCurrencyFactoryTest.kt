package com.tangem.data.common.currency

import android.net.Uri
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.card.WalletData
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCardCryptoCurrencyFactoryTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val userTokensResponseStore: UserTokensResponseStore = mockk()

    private val factory = DefaultCardCryptoCurrencyFactory(
        demoConfig = DemoConfig(),
        excludedBlockchains = ExcludedBlockchains(),
        userWalletsStore = userWalletsStore,
        userTokensResponseStore = userTokensResponseStore,
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = cryptoCurrencyFactory.ethereum.setCanHandleTokens(value = true)
    private val bitcoin = cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Bitcoin)

    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val iconUri: Uri = mockk()

    @BeforeEach
    fun init() {
        clearMocks(userWalletsStore, userTokensResponseStore, iconUri)

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns iconUri
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateMultiWallet {

        @ParameterizedTest
        @ProvideTestModels
        fun `create currencies in ETH for multi-currency wallet`(model: CreateTestModel.MultiWallet) = runTest {
            // Arrange
            val userWallet = createMultiWallet()
            val userTokensResponse = model.userTokensResponse
            val network = ethereum.network

            coEvery { userWalletsStore.getSyncStrict(key = userWallet.walletId) } returns userWallet
            coEvery { userTokensResponseStore.getSyncOrNull(userWallet.walletId) } returns userTokensResponse

            // Act
            val actual = factory.create(userWalletId = userWallet.walletId, network = network)

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                userWalletsStore.getSyncStrict(key = userWallet.walletId)
                userTokensResponseStore.getSyncOrNull(userWalletId = userWallet.walletId)
            }
        }

        private fun provideTestModels() = listOf(
            CreateTestModel.MultiWallet(userTokensResponse = null, expected = emptyList()),
            CreateTestModel.MultiWallet(userTokensResponse = createUserTokensResponse(), expected = emptyList()),
            CreateTestModel.MultiWallet(
                userTokensResponse = createUserTokensResponse(currencies = listOf(ethereum)),
                expected = listOf(ethereum),
            ),
            CreateTestModel.MultiWallet(
                userTokensResponse = createUserTokensResponse(listOf(element = bitcoin)),
                expected = emptyList(),
            ),
        )

        private fun createUserTokensResponse(currencies: List<CryptoCurrency> = emptyList()): UserTokensResponse {
            return userTokensResponseFactory.createUserTokensResponse(
                currencies = currencies,
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateSingleWallet {

        @ParameterizedTest
        @ProvideTestModels
        fun `create currencies for single-currency wallet (ETH)`(model: CreateTestModel.SingleWallet) = runTest {
            // Arrange
            val userWallet = createSingleWallet()

            coEvery { userWalletsStore.getSyncStrict(key = userWallet.walletId) } returns userWallet

            // Act
            val actual = factory.create(userWalletId = userWallet.walletId, network = model.network)

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                userWalletsStore.getSyncStrict(key = userWallet.walletId)
                userWallet.scanResponse.cardTypesResolver.getBlockchain()
            }

            coVerify(inverse = true) {
                userTokensResponseStore.getSyncOrNull(userWalletId = any())
            }
        }

        private fun provideTestModels() = listOf(
            CreateTestModel.SingleWallet(network = ethereum.network, expected = listOf(ethereum)),
            CreateTestModel.SingleWallet(network = bitcoin.network, expected = emptyList()),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateSingleWalletWithToken {

        @ParameterizedTest
        @ProvideTestModels
        fun `create currencies for single-currency wallet with token (ETH)`(
            model: CreateTestModel.SingleWalletWithToken,
        ) = runTest {
            // Arrange
            val userWallet = createSingleWalletWithToken()

            coEvery { userWalletsStore.getSyncStrict(key = userWallet.walletId) } returns userWallet

            // Act
            val actual = factory.create(userWalletId = userWallet.walletId, network = model.network)

            // Assert
            val primaryToken = if (model.isPrimaryTokenExpected) {
                createPrimaryToken(blockchain = Blockchain.Ethereum)
            } else {
                null
            }

            val expected = listOfNotNull(*model.expected.toTypedArray(), primaryToken)

            Truth.assertThat(actual).isEqualTo(expected)

            coVerifyOrder {
                userWalletsStore.getSyncStrict(key = userWallet.walletId)
                userWallet.scanResponse.cardTypesResolver.getBlockchain()
            }

            coVerify(inverse = true) {
                userTokensResponseStore.getSyncOrNull(userWalletId = any())
            }
        }

        private fun provideTestModels() = listOf(
            CreateTestModel.SingleWalletWithToken(
                network = ethereum.network,
                isPrimaryTokenExpected = true,
                expected = listOf(ethereum),
            ),
            CreateTestModel.SingleWalletWithToken(
                network = bitcoin.network,
                isPrimaryTokenExpected = false,
                expected = emptyList(),
            ),
        )
    }

    sealed interface CreateTestModel {

        val expected: List<CryptoCurrency>

        data class MultiWallet(
            val userTokensResponse: UserTokensResponse?,
            override val expected: List<CryptoCurrency>,
        ) : CreateTestModel

        data class SingleWallet(
            val network: Network,
            override val expected: List<CryptoCurrency>,
        ) : CreateTestModel

        data class SingleWalletWithToken(
            val network: Network,
            val isPrimaryTokenExpected: Boolean,
            override val expected: List<CryptoCurrency>,
        ) : CreateTestModel
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateCurrenciesForMultiCurrencyCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `create currencies in ETH and BTC for multi-currency card`(model: CreateCurrenciesForMultiWalletModel) =
            runTest {
                // Arrange
                val userWallet = model.multiWallet
                val networks = setOf(ethereum.network, bitcoin.network)
                val userTokensResponse = model.userTokensResponse

                coEvery { userTokensResponseStore.getSyncOrNull(userWallet.walletId) } returns userTokensResponse

                // Act
                val actual = runCatching {
                    factory.createCurrenciesForMultiCurrencyCard(userWallet = userWallet, networks = networks)
                }

                // Assert
                actual
                    .onSuccess {
                        val expected = model.expected.getOrNull()!!

                        Truth.assertThat(it).isEqualTo(expected)
                    }
                    .onFailure {
                        val exception = model.expected.exceptionOrNull()!!

                        Truth.assertThat(it).isInstanceOf(exception::class.java)
                        Truth.assertThat(it).hasMessageThat().isEqualTo(exception.message)
                    }
            }

        private fun provideTestModels() = listOf(
            CreateCurrenciesForMultiWalletModel(
                multiWallet = createMultiWallet(),
                userTokensResponse = null,
                expected = Result.success(emptyMap()),
            ),
            CreateCurrenciesForMultiWalletModel(
                multiWallet = createMultiWallet(),
                userTokensResponse = createUserTokensResponse(),
                expected = Result.success(emptyMap()),
            ),
            CreateCurrenciesForMultiWalletModel(
                multiWallet = createMultiWallet(),
                userTokensResponse = createUserTokensResponse(currencies = listOf(bitcoin, ethereum)),
                expected = mapOf(
                    bitcoin.network to listOf(bitcoin),
                    ethereum.network to listOf(ethereum),
                ).let(Result.Companion::success),
            ),
            CreateCurrenciesForMultiWalletModel(
                multiWallet = createSingleWallet(),
                userTokensResponse = null,
                expected = Result.failure(IllegalArgumentException("It isn't multi-currency wallet")),
            ),
            CreateCurrenciesForMultiWalletModel(
                multiWallet = createSingleWalletWithToken(),
                userTokensResponse = null,
                expected = Result.failure(IllegalArgumentException("It isn't multi-currency wallet")),
            ),
        )
    }

    data class CreateCurrenciesForMultiWalletModel(
        val multiWallet: UserWallet,
        val userTokensResponse: UserTokensResponse?,
        val expected: Result<Map<Network, List<CryptoCurrency>>>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateDefaultCoinsForMultiCurrencyCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `create default coins for multi currency card`(model: CreateDefaultForMultiWalletModel) = runTest {
            // Arrange
            val multiWallet = model.multiWallet

            // Act
            val actual = factory.createDefaultCoinsForMultiCurrencyCard(scanResponse = multiWallet.scanResponse)

            // Assert
            val expected = model.expected

            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            // PROD card
            CreateDefaultForMultiWalletModel(multiWallet = createMultiWallet(), expected = listOf(bitcoin, ethereum)),
            // TEST card
            CreateDefaultForMultiWalletModel(
                multiWallet = createMultiWallet(cardId = "FF99", batchId = "99FF"),
                expected = listOf(
                    cryptoCurrencyFactory.createCoin(blockchain = Blockchain.BitcoinTestnet),
                    cryptoCurrencyFactory.createCoin(blockchain = Blockchain.EthereumTestnet).setCanHandleTokens(true),
                ),
            ),
            // // DEMO card
            CreateDefaultForMultiWalletModel(
                multiWallet = createMultiWallet(cardId = "AC01000000041225"),
                expected = listOf(
                    bitcoin,
                    ethereum,
                    cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Dogecoin),
                    cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Solana),
                ),
            ),
        )
    }

    data class CreateDefaultForMultiWalletModel(val multiWallet: UserWallet.Cold, val expected: List<CryptoCurrency>)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreatePrimaryCurrencyForSingleCurrencyCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `create primary currency for single currency card`(model: CreatePrimaryCurrencyForSingleWalletModel) =
            runTest {
                // Arrange
                val singleWallet = model.singleWallet

                // Act
                val actual = runCatching {
                    factory.createPrimaryCurrencyForSingleCurrencyCard(scanResponse = singleWallet.scanResponse)
                }

                // Assert
                actual
                    .onSuccess {
                        Truth.assertThat(actual).isEqualTo(model.expected)
                    }
                    .onFailure {
                        val exception = model.expected.exceptionOrNull()!!

                        Truth.assertThat(actual.exceptionOrNull()).isInstanceOf(exception::class.java)
                        Truth.assertThat(actual.exceptionOrNull()).hasMessageThat().isEqualTo(exception.message)
                    }
            }

        private fun provideTestModels() = listOf(
            CreatePrimaryCurrencyForSingleWalletModel(
                singleWallet = createSingleWallet(),
                expected = Result.success(ethereum),
            ),
            CreatePrimaryCurrencyForSingleWalletModel(
                singleWallet = createSingleWallet(batchId = ""),
                expected = Result.failure(IllegalArgumentException("Coin for the single currency card cannot be null")),
            ),
            CreatePrimaryCurrencyForSingleWalletModel(
                singleWallet = createSingleWallet(addWalletData = true),
                expected = Result.failure(IllegalArgumentException("It isn't single-currency wallet")),
            ),
        )
    }

    data class CreatePrimaryCurrencyForSingleWalletModel(
        val singleWallet: UserWallet.Cold,
        val expected: Result<CryptoCurrency>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateCurrenciesForSingleCurrencyCardWithToken {

        @ParameterizedTest
        @ProvideTestModels
        fun `create currencies for single currency card with token`(model: CreateForSingleWalletWithTokenModel) =
            runTest {
                // Arrange
                val userWallet = model.singleWalletWithToken

                // Act
                val actual = runCatching {
                    factory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = userWallet.scanResponse)
                }

                // Assert
                actual
                    .onSuccess {
                        val primaryToken = if (model.isPrimaryTokenExpected) {
                            createPrimaryToken(blockchain = Blockchain.Ethereum)
                        } else {
                            null
                        }

                        val expected = listOfNotNull(*model.expected.getOrNull()!!.toTypedArray(), primaryToken)

                        Truth.assertThat(it).isEqualTo(expected)
                    }
                    .onFailure {
                        val exception = model.expected.exceptionOrNull()!!

                        Truth.assertThat(it).isInstanceOf(exception::class.java)
                        Truth.assertThat(it).hasMessageThat().isEqualTo(exception.message)
                    }
            }

        private fun provideTestModels() = listOf(
            CreateForSingleWalletWithTokenModel(
                singleWalletWithToken = UserWallet.Cold(
                    name = "NODL",
                    walletId = UserWalletId("011"),
                    cardsInWallet = setOf(),
                    isMultiCurrency = false,
                    scanResponse = MockScanResponseFactory.create(
                        cardConfig = GenericCardConfig(maxWalletCount = 2),
                        derivedKeys = emptyMap(),
                    ).copy(
                        productType = ProductType.Note,
                        walletData = WalletData(
                            blockchain = "",
                            token = WalletData.Token(
                                name = "Ethereum",
                                symbol = "ETH",
                                contractAddress = "0x",
                                decimals = 8,
                            ),
                        ),
                    ),
                    hasBackupError = false,
                ),
                expected = Result.failure(IllegalArgumentException("Coin for the single currency card cannot be null")),
            ),
            CreateForSingleWalletWithTokenModel(
                singleWalletWithToken = createSingleWalletWithToken(),
                isPrimaryTokenExpected = true,
                expected = Result.success(listOf(ethereum)),
            ),
        )
    }

    data class CreateForSingleWalletWithTokenModel(
        val singleWalletWithToken: UserWallet.Cold,
        val isPrimaryTokenExpected: Boolean = false,
        val expected: Result<List<CryptoCurrency>>,
    )

    private fun createMultiWallet(cardId: String? = null, batchId: String? = null): UserWallet.Cold {
        return UserWallet.Cold(
            name = "Wallet 1",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = true,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ).let {
                it.copy(
                    card = it.card.copy(
                        cardId = cardId ?: it.card.cardId,
                        batchId = batchId ?: it.card.batchId,
                    ),
                )
            },
            hasBackupError = false,
        )
    }

    private fun createSingleWallet(batchId: String? = null, addWalletData: Boolean = false): UserWallet.Cold {
        return UserWallet.Cold(
            name = "Note",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ).let {
                it.copy(
                    card = it.card.copy(batchId = batchId ?: "AB10"),
                    productType = ProductType.Note,
                    walletData = if (addWalletData) {
                        WalletData(
                            blockchain = "ETH",
                            token = WalletData.Token(
                                name = "Ethereum",
                                symbol = "ETH",
                                contractAddress = "0x",
                                decimals = 8,
                            ),
                        )
                    } else {
                        it.walletData
                    },
                )
            },
            hasBackupError = false,
        )
    }

    private fun createSingleWalletWithToken(): UserWallet.Cold {
        return UserWallet.Cold(
            name = "NODL",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ).copy(
                productType = ProductType.Note,
                walletData = WalletData(
                    blockchain = "ETH",
                    token = WalletData.Token(
                        name = "Ethereum",
                        symbol = "ETH",
                        contractAddress = "0x",
                        decimals = 8,
                    ),
                ),
            ),
            hasBackupError = false,
        )
    }

    private fun createPrimaryToken(blockchain: Blockchain): CryptoCurrency.Token {
        val userWallet = createSingleWalletWithToken()

        return CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains()).createToken(
            sdkToken = userWallet.scanResponse.cardTypesResolver.getPrimaryToken()!!,
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = userWallet.scanResponse,
        )!!
    }

    private fun createUserTokensResponse(currencies: List<CryptoCurrency> = emptyList()): UserTokensResponse {
        return userTokensResponseFactory.createUserTokensResponse(
            currencies = currencies,
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )
    }

    private companion object {

        fun CryptoCurrency.setCanHandleTokens(value: Boolean): CryptoCurrency {
            return when (this) {
                is CryptoCurrency.Coin -> copy(network = network.copy(canHandleTokens = value))
                is CryptoCurrency.Token -> copy(network = network.copy(canHandleTokens = value))
            }
        }
    }
}