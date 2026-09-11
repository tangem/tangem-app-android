package com.tangem.data.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPredictionBalanceRefresherTest {

    private val statusStore: PredictionAccountStatusStore = mockk(relaxed = true)
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val balanceInteractor: GetPolymarketBalanceInteractor = mockk()
    private val statusFetcher: PredictionAccountStatusFetcher = mockk()

    private val refresher = DefaultPredictionBalanceRefresher(
        statusStore = statusStore,
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketBalanceInteractor = balanceInteractor,
        statusFetcher = statusFetcher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(statusStore, deriveAddresses, balanceInteractor, statusFetcher)
    }

    @Test
    fun `GIVEN active status WHEN refresh THEN balance patched with actual source`() = runTest {
        // Arrange
        val active = createActive(balance = BigDecimal("1.00"), source = StatusSource.ONLY_CACHE)
        coEvery { statusStore.getSyncOrNull(WALLET_ID) } returns active
        coEvery { deriveAddresses.stored(WALLET_ID) } returns ADDRESSES
        coEvery { balanceInteractor(ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("3.50"), allowances = null).right()

        // Act
        val result = refresher.refresh(WALLET_ID)

        // Assert
        assertEitherRight(result)
        coVerify(exactly = 1) {
            statusStore.store(WALLET_ID, active.copy(balance = BigDecimal("3.50"), source = StatusSource.ACTUAL))
        }
        coVerify(exactly = 0) { statusFetcher(any()) }
    }

    @Test
    fun `GIVEN non-active status WHEN refresh THEN delegates to the full fetcher`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(WALLET_ID) } returns PredictionAccountStatusValue.Loading
        coEvery { statusFetcher(PredictionAccountStatusFetcher.Params(WALLET_ID)) } returns Unit.right()

        // Act
        val result = refresher.refresh(WALLET_ID)

        // Assert
        assertEitherRight(result)
        coVerify(exactly = 1) { statusFetcher(PredictionAccountStatusFetcher.Params(WALLET_ID)) }
        coVerify(exactly = 0) { statusStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN balance read fails WHEN refresh THEN left and store untouched`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(WALLET_ID) } returns createActive(BigDecimal.ONE, StatusSource.ACTUAL)
        coEvery { deriveAddresses.stored(WALLET_ID) } returns ADDRESSES
        coEvery { balanceInteractor(ADDRESSES) } returns PolymarketAuthError.Network.left()

        // Act
        val result = refresher.refresh(WALLET_ID)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { statusStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN no stored addresses WHEN refresh THEN left and store untouched`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(WALLET_ID) } returns createActive(BigDecimal.ONE, StatusSource.ACTUAL)
        coEvery { deriveAddresses.stored(WALLET_ID) } returns null

        // Act
        val result = refresher.refresh(WALLET_ID)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { statusStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN deposit address written during the balance read WHEN refresh THEN the fresh address is kept`() =
        runTest {
            // Arrange
            val activeWithoutAddress = createActive(BigDecimal.ONE, StatusSource.ONLY_CACHE, depositAddress = null)
            val activeWithAddress = activeWithoutAddress.copy(depositAddress = FRESH_ADDRESS)
            coEvery { statusStore.getSyncOrNull(WALLET_ID) } returnsMany
                listOf(activeWithoutAddress, activeWithAddress)
            coEvery { deriveAddresses.stored(WALLET_ID) } returns ADDRESSES
            coEvery { balanceInteractor(ADDRESSES) } returns
                PolymarketBalanceAllowance(balance = BigDecimal("3.50"), allowances = null).right()

            // Act
            val result = refresher.refresh(WALLET_ID)

            // Assert
            assertEitherRight(result)
            coVerify(exactly = 1) {
                statusStore.store(
                    WALLET_ID,
                    activeWithAddress.copy(balance = BigDecimal("3.50"), source = StatusSource.ACTUAL),
                )
            }
        }

    private fun createActive(
        balance: BigDecimal,
        source: StatusSource,
        depositAddress: String? = DEPOSIT_ADDRESS,
    ) = PredictionAccountStatusValue.Active(
        source = source,
        balance = balance,
        fiatRate = null,
        depositCurrency = DEPOSIT_CURRENCY,
        depositAddress = depositAddress,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val ADDRESSES: PolymarketAddresses = mockk()
        val DEPOSIT_CURRENCY: CryptoCurrency.Token = mockk()

        const val DEPOSIT_ADDRESS = "0xbridge"
        const val FRESH_ADDRESS = "0xfresh"
    }
}