package com.tangem.data.moonpay

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.moonpay.MoonPayCurrencies
import com.tangem.grow.datasource.moonpay.MoonPayCurrenciesMetadata
import com.tangem.grow.datasource.moonpay.MoonPayUserStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMoonPaySellRepositoryTest {

    private val api: MoonPayApi = mockk()
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private var userWallet: UserWallet? = MockUserWalletFactory.create()

    private lateinit var repository: DefaultMoonPaySellRepository

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
        userWallet = MockUserWalletFactory.create()
        repository = DefaultMoonPaySellRepository(
            api = api,
            apiKey = API_KEY,
            secretKey = SECRET_KEY,
            userWalletProvider = { userWallet },
        )
    }

    private suspend fun updateWithEthereumListing() {
        coEvery { api.getUserStatus(API_KEY) } returns createUserStatus()
        coEvery { api.getCurrencies(API_KEY) } returns listOf(
            createMoonPayCurrency(code = "eth", networkCode = "ethereum", contractAddress = null),
            createMoonPayCurrency(code = "usdc", networkCode = "ethereum", contractAddress = TOKEN_CONTRACT_ADDRESS),
        )
        repository.update()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailableForSell {

        @Test
        fun `GIVEN listing never loaded WHEN availableForSell THEN returns false`() = runTest {
            // Act
            val actual = repository.availableForSell(cryptoCurrencyFactory.ethereum)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN no selected user wallet WHEN availableForSell THEN returns false`() = runTest {
            // Arrange
            updateWithEthereumListing()
            userWallet = null

            // Act
            val actual = repository.availableForSell(cryptoCurrencyFactory.ethereum)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN Start2Coin wallet WHEN availableForSell THEN returns false`() = runTest {
            // Arrange
            updateWithEthereumListing()
            userWallet = createStart2CoinUserWallet()

            // Act
            val actual = repository.availableForSell(cryptoCurrencyFactory.ethereum)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN blockchain unknown to MoonPay WHEN availableForSell THEN returns false`() = runTest {
            // Arrange
            updateWithEthereumListing()
            val unknownCoin = createCoinWithUnknownBlockchain()

            // Act
            val actual = repository.availableForSell(unknownCoin)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN sellable coin listed by MoonPay WHEN availableForSell THEN returns true`() = runTest {
            // Arrange
            updateWithEthereumListing()

            // Act
            val actual = repository.availableForSell(cryptoCurrencyFactory.ethereum)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN coin not listed by MoonPay WHEN availableForSell THEN returns false`() = runTest {
            // Arrange
            updateWithEthereumListing()

            // Act
            val actual = repository.availableForSell(cryptoCurrencyFactory.bitcoin)

            // Assert
            assertThat(actual).isFalse()
        }

        @Test
        fun `GIVEN token matching the listed contract address WHEN availableForSell THEN returns true`() = runTest {
            // Arrange
            updateWithEthereumListing()
            val token = cryptoCurrencyFactory.createToken(
                blockchain = Blockchain.Ethereum,
                contractAddress = TOKEN_CONTRACT_ADDRESS,
            )

            // Act
            val actual = repository.availableForSell(token)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN token with a different contract address WHEN availableForSell THEN returns false`() = runTest {
            // Arrange
            updateWithEthereumListing()
            val token = cryptoCurrencyFactory.createToken(
                blockchain = Blockchain.Ethereum,
                contractAddress = "0xDeadBeef",
            )

            // Act
            val actual = repository.availableForSell(token)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetUrl {

        @Test
        fun `GIVEN testnet currency WHEN getUrl THEN returns the blockchain testnet top-up url`() = runTest {
            // Arrange
            val testnetCoin = cryptoCurrencyFactory.createCoin(Blockchain.EthereumTestnet)

            // Act
            val actual = repository.getUrl(
                cryptoCurrency = testnetCoin,
                fiatCurrencyName = "USD",
                walletAddress = WALLET_ADDRESS,
                isDarkTheme = false,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isEqualTo(Blockchain.EthereumTestnet.getTestnetTopUpUrl())
        }

        @Test
        fun `GIVEN mainnet currency unknown to MoonPay WHEN getUrl THEN returns null`() = runTest {
            // Arrange
            val unknownCoin = createCoinWithUnknownBlockchain()

            // Act
            val actual = repository.getUrl(
                cryptoCurrency = unknownCoin,
                fiatCurrencyName = "USD",
                walletAddress = WALLET_ADDRESS,
                isDarkTheme = false,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN mainnet currency absent from the sell listing WHEN getUrl THEN returns null`() = runTest {
            // Arrange
            updateWithEthereumListing()

            // Act
            val actual = repository.getUrl(
                cryptoCurrency = cryptoCurrencyFactory.bitcoin,
                fiatCurrencyName = "USD",
                walletAddress = WALLET_ADDRESS,
                isDarkTheme = false,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isNull()
        }
    }

    private fun createCoinWithUnknownBlockchain(): CryptoCurrency.Coin {
        val ethereum = cryptoCurrencyFactory.ethereum
        val bogusNetwork = ethereum.network.copy(
            id = ethereum.network.id.copy(rawId = Network.RawID(value = "totally-unsupported-network-id")),
        )
        return ethereum.copy(network = bogusNetwork)
    }

    private companion object {

        const val API_KEY = "test-api-key"
        const val SECRET_KEY = "test-secret-key"
        const val WALLET_ADDRESS = "0xWalletAddress"
        const val REQUEST_ID = "request-id-1"
        const val TOKEN_CONTRACT_ADDRESS = "0xTokenContractAddress"

        fun createUserStatus() = MoonPayUserStatus(
            isBuyAllowed = true,
            isSellAllowed = true,
            isMoonpayAllowed = true,
            countryCode = "DEU",
            stateCode = "",
        )

        fun createMoonPayCurrency(code: String, networkCode: String, contractAddress: String?) = MoonPayCurrencies(
            type = "crypto",
            code = code,
            supportsLiveMode = true,
            isSuspended = false,
            isSupportedInUS = true,
            isSellSupported = true,
            notAllowedUSStates = emptyList(),
            metadata = MoonPayCurrenciesMetadata(contractAddress = contractAddress, networkCode = networkCode),
        )

        fun createStart2CoinUserWallet(): UserWallet.Cold {
            val base = MockUserWalletFactory.create()
            return base.copy(
                scanResponse = base.scanResponse.copy(
                    card = base.scanResponse.card.copy(
                        issuer = CardDTO.Issuer(name = "start2coin", publicKey = ByteArray(0)),
                    ),
                ),
            )
        }
    }
}