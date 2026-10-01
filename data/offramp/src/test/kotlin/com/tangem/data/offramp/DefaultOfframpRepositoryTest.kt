package com.tangem.data.offramp

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.data.offramp.model.PendingOfframpEntry
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.moonpay.MoonPayCurrencies
import com.tangem.grow.datasource.moonpay.MoonPayCurrenciesMetadata
import com.tangem.grow.datasource.moonpay.MoonPayUserStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultOfframpRepositoryTest {

    private val api: MoonPayApi = mockk()
    private val appThemeModeHolder: AppThemeModeHolder = mockk()
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private var userWallet: UserWallet? = MockUserWalletFactory.create()

    private val pendingStoreState = MutableStateFlow<List<PendingOfframpEntry>>(emptyList())
    private val pendingOfframpStore = object : DataStore<List<PendingOfframpEntry>> {
        override val data = pendingStoreState
        override suspend fun updateData(
            transform: suspend (t: List<PendingOfframpEntry>) -> List<PendingOfframpEntry>,
        ): List<PendingOfframpEntry> {
            val updated = transform(pendingStoreState.value)
            pendingStoreState.value = updated
            return updated
        }
    }

    private lateinit var repository: DefaultOfframpRepository

    @BeforeEach
    fun resetMocks() {
        clearMocks(api, appThemeModeHolder)
        every { appThemeModeHolder.isDarkThemeActive } returns false
        userWallet = MockUserWalletFactory.create()
        pendingStoreState.value = emptyList()
        repository = DefaultOfframpRepository(
            api = api,
            credentials = MoonPayCredentials(apiKey = API_KEY, secretKey = SECRET_KEY),
            userWalletProvider = { userWallet },
            appThemeModeHolder = appThemeModeHolder,
            pendingOfframpStore = pendingOfframpStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
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
    inner class GetOfframpUrl {

        @Test
        fun `GIVEN testnet currency WHEN getOfframpUrl THEN returns the blockchain testnet top-up url`() = runTest {
            // Arrange
            val testnetCoin = cryptoCurrencyFactory.createCoin(Blockchain.EthereumTestnet)

            // Act
            val actual = repository.getOfframpUrl(
                cryptoCurrency = testnetCoin,
                fiatCurrencyCode = "USD",
                walletAddress = WALLET_ADDRESS,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isEqualTo(Blockchain.EthereumTestnet.getTestnetTopUpUrl())
        }

        @Test
        fun `GIVEN mainnet currency unknown to MoonPay WHEN getOfframpUrl THEN returns null`() = runTest {
            // Arrange
            val unknownCoin = createCoinWithUnknownBlockchain()

            // Act
            val actual = repository.getOfframpUrl(
                cryptoCurrency = unknownCoin,
                fiatCurrencyCode = "USD",
                walletAddress = WALLET_ADDRESS,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN mainnet currency absent from the sell listing WHEN getOfframpUrl THEN returns null`() = runTest {
            // Arrange
            updateWithEthereumListing()

            // Act
            val actual = repository.getOfframpUrl(
                cryptoCurrency = cryptoCurrencyFactory.bitcoin,
                fiatCurrencyCode = "USD",
                walletAddress = WALLET_ADDRESS,
                requestId = REQUEST_ID,
            )

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PendingOfframps {

        @Test
        fun `GIVEN registered pending offramp WHEN resolve with matching wallet and currency THEN returns record`() =
            runTest {
                // Arrange
                val storedRequestId = repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

                // Act
                val pending = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, CURRENCY_ID)

                // Assert
                assertThat(pending).isNotNull()
                assertThat(pending?.requestId).isEqualTo(storedRequestId)
                assertThat(pending?.userWalletId).isEqualTo(USER_WALLET_ID)
                assertThat(pending?.currencyId).isEqualTo(CURRENCY_ID)
            }

        @Test
        fun `GIVEN unknown request id WHEN resolve THEN returns null`() = runTest {
            // Arrange
            repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

            // Act
            val actual = repository.resolvePendingOfframp("unknown", USER_WALLET_ID, CURRENCY_ID)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN already resolved pending offramp WHEN resolve again THEN still returns record`() = runTest {
            // Arrange
            val storedRequestId = repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

            // Act - resolving does NOT consume the record; the same redirect may be followed again until it expires
            val first = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, CURRENCY_ID)
            val second = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, CURRENCY_ID)

            // Assert
            assertThat(first).isNotNull()
            assertThat(second).isNotNull()
            assertThat(second?.requestId).isEqualTo(storedRequestId)
        }

        @Test
        fun `GIVEN mismatched currency WHEN resolve THEN returns null and leaves the pending sell resolvable`() =
            runTest {
                // Arrange
                val storedRequestId = repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

                // Act - a tampered redirect with the right request_id but a wrong currency must not match
                val mismatched = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, "ethereum")
                // ...and the legitimate redirect must still resolve afterwards
                val legitimate = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, CURRENCY_ID)

                // Assert
                assertThat(mismatched).isNull()
                assertThat(legitimate).isNotNull()
                assertThat(legitimate?.currencyId).isEqualTo(CURRENCY_ID)
            }

        @Test
        fun `GIVEN mismatched wallet WHEN resolve THEN returns null`() = runTest {
            // Arrange
            val storedRequestId = repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

            // Act
            val actual = repository.resolvePendingOfframp(
                storedRequestId,
                UserWalletId("ffeeddccbbaa9988"),
                CURRENCY_ID,
            )

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN expired pending offramp WHEN resolve THEN returns null`() = runTest {
            // Arrange - seed a record created 2 hours ago (past the 1h expiry)
            val expiredId = "expired-id"
            pendingStoreState.value = listOf(
                PendingOfframpEntry(
                    requestId = expiredId,
                    userWalletId = USER_WALLET_ID.stringValue,
                    currencyId = CURRENCY_ID,
                    createdAt = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
                ),
            )

            // Act
            val pending = repository.resolvePendingOfframp(expiredId, USER_WALLET_ID, CURRENCY_ID)

            // Assert
            assertThat(pending).isNull()
        }

        @Test
        fun `GIVEN registered pending offramps WHEN getAllStoredOfframps THEN returns them without consuming`() =
            runTest {
                // Arrange
                val storedRequestId = repository.registerPendingOfframp(USER_WALLET_ID, CURRENCY_ID)

                // Act
                val stored = repository.getAllStoredOfframps()
                // ...the record must survive the read so it can still be consumed afterwards
                val consumed = repository.resolvePendingOfframp(storedRequestId, USER_WALLET_ID, CURRENCY_ID)

                // Assert
                assertThat(stored).hasSize(1)
                assertThat(stored.single().requestId).isEqualTo(storedRequestId)
                assertThat(stored.single().userWalletId).isEqualTo(USER_WALLET_ID)
                assertThat(stored.single().currencyId).isEqualTo(CURRENCY_ID)
                assertThat(consumed).isNotNull()
            }

        @Test
        fun `GIVEN expired pending offramp WHEN getAllStoredOfframps THEN it is still returned and flagged expired`() =
            runTest {
                // Arrange - seed a record created 2 hours ago (past the 1h expiry)
                val now = System.currentTimeMillis()
                pendingStoreState.value = listOf(
                    PendingOfframpEntry(
                        requestId = "expired-id",
                        userWalletId = USER_WALLET_ID.stringValue,
                        currencyId = CURRENCY_ID,
                        createdAt = now - TimeUnit.HOURS.toMillis(2),
                    ),
                )

                // Act
                val stored = repository.getAllStoredOfframps()

                // Assert
                assertThat(stored).hasSize(1)
                assertThat(stored.single().isExpired(now)).isTrue()
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
        const val CURRENCY_ID = "bitcoin"
        val USER_WALLET_ID = UserWalletId("0011223344556677")

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