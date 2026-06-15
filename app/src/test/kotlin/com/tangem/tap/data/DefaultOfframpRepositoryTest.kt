package com.tangem.tap.data

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.data.model.PendingOfframpEntry
import com.tangem.tap.network.exchangeServices.SellService
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultOfframpRepositoryTest {

    private val sellService: SellService = mockk()
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
    private val repository = DefaultOfframpRepository(
        sellService = sellService,
        pendingOfframpStore = pendingOfframpStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val cryptoCurrency: CryptoCurrency = mockk()
    private val fiatCurrencyCode = "USD"
    private val walletAddress = "0x1234567890abcdef"
    private val requestId = "request-id-001"
    private val userWalletId = UserWalletId("0011223344556677")
    private val currencyId = "bitcoin"

    @BeforeEach
    fun setUp() {
        mockkObject(MutableAppThemeModeHolder)
        pendingStoreState.value = emptyList()
    }

    @AfterEach
    fun tearDown() {
        clearMocks(sellService)
        unmockkObject(MutableAppThemeModeHolder)
    }

    @Test
    fun `getOfframpUrl should return url when sellService returns url with light theme`() {
        // Arrange
        val expectedUrl = "https://moonpay.com/sell?address=$walletAddress&theme=light"
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns false
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
                requestId = requestId,
            )
        } returns expectedUrl

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
            requestId = requestId,
        )

        // Assert
        assertThat(result).isEqualTo(expectedUrl)
        verify(exactly = 1) {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
                requestId = requestId,
            )
        }
    }

    @Test
    fun `getOfframpUrl should return url when sellService returns url with dark theme`() {
        // Arrange
        val expectedUrl = "https://moonpay.com/sell?address=$walletAddress&theme=dark"
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns true
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = true,
                requestId = requestId,
            )
        } returns expectedUrl

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
            requestId = requestId,
        )

        // Assert
        assertThat(result).isEqualTo(expectedUrl)
        verify(exactly = 1) {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = true,
                requestId = requestId,
            )
        }
    }

    @Test
    fun `getOfframpUrl should return null when sellService returns null`() {
        // Arrange
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns false
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
                requestId = requestId,
            )
        } returns null

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
            requestId = requestId,
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN registered pending offramp WHEN consume with matching wallet and currency THEN returns record`() =
        runTest {
            // Arrange
            val storedRequestId = repository.registerPendingOfframp(userWalletId, currencyId)

            // Act
            val pending = repository.consumePendingOfframp(storedRequestId, userWalletId, currencyId)

            // Assert
            assertThat(pending).isNotNull()
            assertThat(pending?.requestId).isEqualTo(storedRequestId)
            assertThat(pending?.userWalletId).isEqualTo(userWalletId)
            assertThat(pending?.currencyId).isEqualTo(currencyId)
        }

    @Test
    fun `GIVEN unknown request id WHEN consume THEN returns null`() = runTest {
        repository.registerPendingOfframp(userWalletId, currencyId)

        assertThat(repository.consumePendingOfframp("unknown", userWalletId, currencyId)).isNull()
    }

    @Test
    fun `GIVEN already consumed pending offramp WHEN consume again THEN returns null`() = runTest {
        // Arrange
        val storedRequestId = repository.registerPendingOfframp(userWalletId, currencyId)

        // Act
        val first = repository.consumePendingOfframp(storedRequestId, userWalletId, currencyId)
        val second = repository.consumePendingOfframp(storedRequestId, userWalletId, currencyId)

        // Assert
        assertThat(first).isNotNull()
        assertThat(second).isNull()
    }

    @Test
    fun `GIVEN mismatched currency WHEN consume THEN returns null and does NOT burn the pending sell`() = runTest {
        // Arrange
        val storedRequestId = repository.registerPendingOfframp(userWalletId, currencyId)

        // Act — a tampered redirect with the right request_id but a wrong currency must not consume the token
        val mismatched = repository.consumePendingOfframp(storedRequestId, userWalletId, currencyId = "ethereum")
        // ...so the legitimate redirect can still succeed afterwards
        val legitimate = repository.consumePendingOfframp(storedRequestId, userWalletId, currencyId)

        // Assert
        assertThat(mismatched).isNull()
        assertThat(legitimate).isNotNull()
        assertThat(legitimate?.currencyId).isEqualTo(currencyId)
    }

    @Test
    fun `GIVEN mismatched wallet WHEN consume THEN returns null`() = runTest {
        val storedRequestId = repository.registerPendingOfframp(userWalletId, currencyId)

        val result = repository.consumePendingOfframp(storedRequestId, UserWalletId("ffeeddccbbaa9988"), currencyId)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN expired pending offramp WHEN consume THEN returns null`() = runTest {
        // Arrange — seed a record created 2 hours ago (past the 1h expiry)
        val expiredId = "expired-id"
        pendingStoreState.value = listOf(
            PendingOfframpEntry(
                requestId = expiredId,
                userWalletId = userWalletId.stringValue,
                currencyId = currencyId,
                createdAt = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
            ),
        )

        // Act
        val pending = repository.consumePendingOfframp(expiredId, userWalletId, currencyId)

        // Assert
        assertThat(pending).isNull()
    }
}