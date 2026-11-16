package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyMetadataCleanerTest {

    private val firstCurrencyDataCleaner: CryptoCurrencyDataCleaner = mockk(relaxUnitFun = true)
    private val secondCurrencyDataCleaner: CryptoCurrencyDataCleaner = mockk(relaxUnitFun = true)
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()
    private val networksCleaner: NetworksCleaner = mockk(relaxUnitFun = true)
    private val stakingCleaner: StakingCleaner = mockk(relaxUnitFun = true)
    private val nftCleaner: NFTCleaner = mockk(relaxUnitFun = true)

    private val cleaner = CryptoCurrencyMetadataCleaner(
        currencyDataCleaners = setOf(firstCurrencyDataCleaner, secondCurrencyDataCleaner),
        tokensFeatureToggles = tokensFeatureToggles,
        networksCleaner = networksCleaner,
        stakingCleaner = stakingCleaner,
        nftCleaner = nftCleaner,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val coin = cryptoCurrencyFactory.ethereum
    private val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)

    @AfterEach
    fun tearDown() {
        clearMocks(
            firstCurrencyDataCleaner,
            secondCurrencyDataCleaner,
            tokensFeatureToggles,
            networksCleaner,
            stakingCleaner,
            nftCleaner,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleOn {

        @BeforeEach
        fun enableToggle() {
            every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns true
        }

        @Test
        fun `GIVEN empty currencies WHEN invoke THEN no cleaner is called`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, currencies = emptyList())

            // Assert
            coVerify(exactly = 0) {
                firstCurrencyDataCleaner(userWalletId = any(), currencies = any())
                secondCurrencyDataCleaner(userWalletId = any(), currencies = any())
            }
        }

        @Test
        fun `GIVEN currencies WHEN invoke THEN every registered cleaner is called and legacy ones are not`() =
            runTest {
                // Arrange
                val currencies = listOf(coin, token)

                // Act
                cleaner(userWalletId = userWalletId, currencies = currencies)

                // Assert
                coVerify(exactly = 1) {
                    firstCurrencyDataCleaner(userWalletId = userWalletId, currencies = currencies)
                    secondCurrencyDataCleaner(userWalletId = userWalletId, currencies = currencies)
                }
                coVerify(exactly = 0) {
                    networksCleaner(userWalletId = any(), currencies = any())
                    stakingCleaner(userWalletId = any(), currencies = any())
                    nftCleaner(userWalletId = any(), networks = any())
                }
            }

        @Test
        fun `GIVEN single currency WHEN invoke THEN cleaners receive it as a list`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, currency = coin)

            // Assert
            coVerify(exactly = 1) {
                firstCurrencyDataCleaner(userWalletId = userWalletId, currencies = listOf(coin))
                secondCurrencyDataCleaner(userWalletId = userWalletId, currencies = listOf(coin))
            }
        }

        @Test
        fun `GIVEN one cleaner throws WHEN invoke THEN other cleaners still run`() = runTest {
            // Arrange
            val currencies = listOf(coin)
            coEvery {
                firstCurrencyDataCleaner(userWalletId = any(), currencies = any())
            } throws IllegalStateException()

            // Act
            cleaner(userWalletId = userWalletId, currencies = currencies)

            // Assert
            coVerify(exactly = 1) { secondCurrencyDataCleaner(userWalletId = userWalletId, currencies = currencies) }
        }

        @Test
        fun `GIVEN wallets WHEN clear THEN every registered cleaner clears them and legacy ones are not`() =
            runTest {
                // Arrange
                val ids = listOf(userWalletId, UserWalletId("022"))

                // Act
                cleaner.clear(userWalletIds = ids)

                // Assert
                coVerify(exactly = 1) {
                    firstCurrencyDataCleaner.clear(ids)
                    secondCurrencyDataCleaner.clear(ids)
                }
                coVerify(exactly = 0) {
                    networksCleaner.clear(any())
                    stakingCleaner.clear(any())
                }
            }

        @Test
        fun `GIVEN one cleaner throws WHEN clear THEN other cleaners still run`() = runTest {
            // Arrange
            val ids = listOf(userWalletId)
            coEvery { firstCurrencyDataCleaner.clear(any()) } throws IllegalStateException()

            // Act
            cleaner.clear(userWalletIds = ids)

            // Assert
            coVerify(exactly = 1) { secondCurrencyDataCleaner.clear(ids) }
        }

        @Test
        fun `GIVEN empty list WHEN clear THEN no cleaners are called`() = runTest {
            // Act
            cleaner.clear(userWalletIds = emptyList())

            // Assert
            coVerify(exactly = 0) {
                firstCurrencyDataCleaner.clear(any())
                secondCurrencyDataCleaner.clear(any())
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleOff {

        @BeforeEach
        fun disableToggle() {
            every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns false
        }

        @Test
        fun `GIVEN empty currencies WHEN invoke THEN no cleaner is called`() = runTest {
            // Act
            cleaner(userWalletId = userWalletId, currencies = emptyList())

            // Assert
            coVerify(exactly = 0) {
                networksCleaner(userWalletId = any(), currencies = any())
                stakingCleaner(userWalletId = any(), currencies = any())
                nftCleaner(userWalletId = any(), networks = any())
            }
        }

        @Test
        fun `GIVEN currencies WHEN invoke THEN legacy cleaners are called and registered ones are not`() =
            runTest {
                // Arrange
                val currencies = listOf(coin, token)

                // Act
                cleaner(userWalletId = userWalletId, currencies = currencies)

                // Assert
                coVerify(exactly = 1) {
                    networksCleaner(userWalletId = userWalletId, currencies = currencies)
                    stakingCleaner(userWalletId = userWalletId, currencies = currencies)
                    nftCleaner(userWalletId = userWalletId, networks = setOf(coin.network, token.network))
                }
                coVerify(exactly = 0) {
                    firstCurrencyDataCleaner(userWalletId = any(), currencies = any())
                    secondCurrencyDataCleaner(userWalletId = any(), currencies = any())
                }
            }

        @Test
        fun `GIVEN wallets WHEN clear THEN networks and staking cleared once with all ids`() = runTest {
            // Arrange
            val ids = listOf(userWalletId, UserWalletId("022"))

            // Act
            cleaner.clear(userWalletIds = ids)

            // Assert
            coVerify(exactly = 1) {
                networksCleaner.clear(ids)
                stakingCleaner.clear(ids)
            }
            coVerify(exactly = 0) {
                firstCurrencyDataCleaner.clear(any())
                secondCurrencyDataCleaner.clear(any())
            }
        }

        @Test
        fun `GIVEN empty list WHEN clear THEN no cleaners are called`() = runTest {
            // Act
            cleaner.clear(userWalletIds = emptyList())

            // Assert
            coVerify(exactly = 0) {
                networksCleaner.clear(any())
                stakingCleaner.clear(any())
            }
        }
    }
}