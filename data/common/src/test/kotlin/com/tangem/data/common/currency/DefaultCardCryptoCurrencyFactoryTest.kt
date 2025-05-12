package com.tangem.data.common.currency

import android.net.Uri
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.card.WalletData
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultCardCryptoCurrencyFactoryTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val userTokensResponseStore: UserTokensResponseStore = mockk()

    private val factory = DefaultCardCryptoCurrencyFactory(
        demoConfig = DemoConfig(),
        excludedBlockchains = ExcludedBlockchains(),
        userWalletsStore = userWalletsStore,
        userTokensResponseStore = userTokensResponseStore,
    )

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @Test
    fun `test create if userTokensResponse is not empty`() = runTest {
        val multiWallet = createMultiWallet()

        val userTokensResponse = UserTokensResponseFactory().createUserTokensResponse(
            currencies = listOf(ethereum),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        coEvery { userWalletsStore.getSyncStrict(key = multiWallet.walletId) } returns multiWallet
        coEvery { userTokensResponseStore.getSyncOrNull(multiWallet.walletId) } returns userTokensResponse

        val actual = factory.create(userWalletId = multiWallet.walletId, network = ethereum.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = multiWallet.walletId)
            userTokensResponseStore.getSyncOrNull(multiWallet.walletId)
        }

        val expected = listOf(ethereum)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if userTokensResponse is empty`() = runTest {
        val multiWallet = createMultiWallet()

        val userTokensResponse = UserTokensResponseFactory().createUserTokensResponse(
            currencies = listOf(),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        coEvery { userWalletsStore.getSyncStrict(key = multiWallet.walletId) } returns multiWallet
        coEvery { userTokensResponseStore.getSyncOrNull(multiWallet.walletId) } returns userTokensResponse

        val actual = factory.create(userWalletId = multiWallet.walletId, network = ethereum.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = multiWallet.walletId)
            userTokensResponseStore.getSyncOrNull(multiWallet.walletId)
        }

        val expected = emptyList<CryptoCurrency>()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if userTokensResponse is null`() = runTest {
        val multiWallet = createMultiWallet()

        coEvery { userWalletsStore.getSyncStrict(key = multiWallet.walletId) } returns multiWallet
        coEvery { userTokensResponseStore.getSyncOrNull(multiWallet.walletId) } returns null

        val actual = factory.create(userWalletId = multiWallet.walletId, network = ethereum.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = multiWallet.walletId)
            userTokensResponseStore.getSyncOrNull(multiWallet.walletId)
        }

        val expected = emptyList<CryptoCurrency>()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if userTokensResponse does not contain currency of selected network`() = runTest {
        val multiWallet = createMultiWallet()

        val userTokensResponse = UserTokensResponseFactory().createUserTokensResponse(
            currencies = listOf(bitcoin),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        coEvery { userWalletsStore.getSyncStrict(key = multiWallet.walletId) } returns multiWallet
        coEvery { userTokensResponseStore.getSyncOrNull(multiWallet.walletId) } returns userTokensResponse

        val actual = factory.create(userWalletId = multiWallet.walletId, network = ethereum.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = multiWallet.walletId)
            userTokensResponseStore.getSyncOrNull(multiWallet.walletId)
        }

        val expected = emptyList<CryptoCurrency>()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if single wallet has another primary network`() = runTest {
        val singleWallet = createSingleWallet()

        coEvery { userWalletsStore.getSyncStrict(key = singleWallet.walletId) } returns singleWallet

        val actual = factory.create(userWalletId = singleWallet.walletId, network = bitcoin.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = singleWallet.walletId)
            singleWallet.scanResponse.cardTypesResolver.getBlockchain()
        }

        val expected = emptyList<CryptoCurrency>()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if card is single wallet`() = runTest {
        val singleWallet = createSingleWallet()

        coEvery { userWalletsStore.getSyncStrict(key = singleWallet.walletId) } returns singleWallet

        val actual = factory.create(userWalletId = singleWallet.walletId, network = ethereum.network)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = singleWallet.walletId)
            singleWallet.scanResponse.cardTypesResolver.getBlockchain()
        }

        val expected = listOf(ethereum)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test create if card is single wallet with token`() = runTest {
        val singleWallet = createSingleWalletWithToken()

        coEvery { userWalletsStore.getSyncStrict(key = singleWallet.walletId) } returns singleWallet

        val actual = factory.create(userWalletId = singleWallet.walletId, network = ethereum.network)

        val token = CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains()).createToken(
            sdkToken = singleWallet.scanResponse.cardTypesResolver.getPrimaryToken()!!,
            blockchain = Blockchain.Ethereum,
            extraDerivationPath = null,
            scanResponse = singleWallet.scanResponse,
        )
        val expected = listOf(ethereum, token)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createDefaultCoinsForMultiCurrencyCard if card is prod`() = runTest {
        val multiWallet = createMultiWallet()

        val actual = factory.createDefaultCoinsForMultiCurrencyCard(scanResponse = multiWallet.scanResponse)

        val expected = listOf(bitcoin, ethereum)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createDefaultCoinsForMultiCurrencyCard if card is test`() = runTest {
        val multiWallet = createMultiWallet().let {
            it.copy(
                scanResponse = it.scanResponse.copy(
                    card = it.scanResponse.card.copy(cardId = "FF99", batchId = "99FF"),
                ),
            )
        }

        val actual = factory.createDefaultCoinsForMultiCurrencyCard(scanResponse = multiWallet.scanResponse)

        val expected = listOf(
            cryptoCurrencyFactory.createCoin(blockchain = Blockchain.BitcoinTestnet),
            cryptoCurrencyFactory.createCoin(blockchain = Blockchain.EthereumTestnet).setCanHandleTokens(true),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createDefaultCoinsForMultiCurrencyCard if card is demo`() = runTest {
        val multiWallet = createMultiWallet().let {
            it.copy(
                scanResponse = it.scanResponse.copy(
                    card = it.scanResponse.card.copy(cardId = "AC01000000041225"),
                ),
            )
        }

        val actual = factory.createDefaultCoinsForMultiCurrencyCard(scanResponse = multiWallet.scanResponse)

        val expected = listOf(
            bitcoin,
            ethereum,
            cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Dogecoin),
            cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Solana),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createPrimaryCurrencyForSingleCurrencyCard if unable to create token`() = runTest {
        val singleWallet = UserWallet(
            name = "Note",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ),
            hasBackupError = false,
        )

        val actual = runCatching {
            factory.createPrimaryCurrencyForSingleCurrencyCard(scanResponse = singleWallet.scanResponse)
        }

        val exception = IllegalArgumentException("Coin for the single currency card cannot be null")

        Truth.assertThat(actual.isFailure).isTrue()
        Truth.assertThat(actual.exceptionOrNull()).isInstanceOf(exception::class.java)
        Truth.assertThat(actual.exceptionOrNull()).hasMessageThat().isEqualTo(exception.message)
    }

    @Test
    fun `test createPrimaryCurrencyForSingleCurrencyCard if primaryToken is null`() = runTest {
        val singleWallet = createSingleWallet()

        val actual = factory.createPrimaryCurrencyForSingleCurrencyCard(scanResponse = singleWallet.scanResponse)

        val expected = ethereum

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createPrimaryCurrencyForSingleCurrencyCard if primaryToken is not null`() = runTest {
        val singleWallet = createSingleWalletWithToken()

        val actual = factory.createPrimaryCurrencyForSingleCurrencyCard(scanResponse = singleWallet.scanResponse)

        val expected = CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains()).createToken(
            sdkToken = singleWallet.scanResponse.cardTypesResolver.getPrimaryToken()!!,
            blockchain = Blockchain.Ethereum,
            extraDerivationPath = null,
            scanResponse = singleWallet.scanResponse,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createCurrenciesForSingleCurrencyCardWithToken if unable to create token`() = runTest {
        val singleWalletWithToken = UserWallet(
            name = "Note",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ),
            hasBackupError = false,
        )

        val actual = runCatching {
            factory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = singleWalletWithToken.scanResponse)
        }

        val exception = IllegalArgumentException("Coin for the single currency card cannot be null")

        Truth.assertThat(actual.isFailure).isTrue()
        Truth.assertThat(actual.exceptionOrNull()).isInstanceOf(exception::class.java)
        Truth.assertThat(actual.exceptionOrNull()).hasMessageThat().isEqualTo(exception.message)
    }

    @Test
    fun `test createCurrenciesForSingleCurrencyCardWithToken if primaryToken is null`() = runTest {
        val singleWalletWithToken = createSingleWallet()

        val actual = factory.createCurrenciesForSingleCurrencyCardWithToken(
            scanResponse = singleWalletWithToken.scanResponse,
        )

        val expected = listOf(ethereum)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test createCurrenciesForSingleCurrencyCardWithToken if primaryToken is not null`() = runTest {
        val singleWalletWithToken = createSingleWalletWithToken()

        val actual = factory.createCurrenciesForSingleCurrencyCardWithToken(
            scanResponse = singleWalletWithToken.scanResponse,
        )

        val token = CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains()).createToken(
            sdkToken = singleWalletWithToken.scanResponse.cardTypesResolver.getPrimaryToken()!!,
            blockchain = Blockchain.Ethereum,
            extraDerivationPath = null,
            scanResponse = singleWalletWithToken.scanResponse,
        )

        val expected = listOf(ethereum, token)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun createMultiWallet(): UserWallet {
        return UserWallet(
            name = "Wallet 1",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = true,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ),
            hasBackupError = false,
        )
    }

    private fun createSingleWallet(): UserWallet {
        return UserWallet(
            name = "Note",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ).let {
                it.copy(
                    card = it.card.copy(batchId = "AB10"),
                    productType = ProductType.Note,
                )
            },
            hasBackupError = false,
        )
    }

    private fun createSingleWalletWithToken(): UserWallet {
        return UserWallet(
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

    private companion object {

        val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

        val ethereum = cryptoCurrencyFactory.ethereum.setCanHandleTokens(value = true)

        val bitcoin = cryptoCurrencyFactory.createCoin(blockchain = Blockchain.Bitcoin)

        fun CryptoCurrency.setCanHandleTokens(value: Boolean): CryptoCurrency {
            return when (this) {
                is CryptoCurrency.Coin -> copy(network = network.copy(canHandleTokens = value))
                is CryptoCurrency.Token -> copy(network = network.copy(canHandleTokens = value))
            }
        }
    }
}