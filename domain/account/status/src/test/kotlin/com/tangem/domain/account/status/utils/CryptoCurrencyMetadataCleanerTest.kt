package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyMetadataCleanerTest {

    private val firstCurrencyDataCleaner: CryptoCurrencyDataCleaner = mockk(relaxUnitFun = true)
    private val secondCurrencyDataCleaner: CryptoCurrencyDataCleaner = mockk(relaxUnitFun = true)

    private val cleaner = CryptoCurrencyMetadataCleaner(
        currencyDataCleaners = setOf(firstCurrencyDataCleaner, secondCurrencyDataCleaner),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val coin = cryptoCurrencyFactory.ethereum
    private val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)

    @AfterEach
    fun tearDown() {
        clearMocks(firstCurrencyDataCleaner, secondCurrencyDataCleaner)
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
    fun `GIVEN currencies WHEN invoke THEN every registered cleaner is called`() = runTest {
        // Arrange
        val currencies = listOf(coin, token)

        // Act
        cleaner(userWalletId = userWalletId, currencies = currencies)

        // Assert
        coVerify(exactly = 1) {
            firstCurrencyDataCleaner(userWalletId = userWalletId, currencies = currencies)
            secondCurrencyDataCleaner(userWalletId = userWalletId, currencies = currencies)
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
    fun `GIVEN wallets WHEN clear THEN every registered cleaner clears them`() = runTest {
        // Arrange
        val ids = listOf(userWalletId, UserWalletId("022"))

        // Act
        cleaner.clear(userWalletIds = ids)

        // Assert
        coVerify(exactly = 1) {
            firstCurrencyDataCleaner.clear(ids)
            secondCurrencyDataCleaner.clear(ids)
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