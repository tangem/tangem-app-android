package com.tangem.domain.polymarket.flow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.swap.models.SwapTransactionListModel
import com.tangem.domain.swap.models.SwapTransactionModel
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

internal class DefaultPredictionFundingWindowTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val swapTransactionRepository: SwapTransactionRepository = mockk()
    private val statusSupplier: PredictionAccountStatusSupplier = mockk()
    private val collateralFactory: PolymarketCollateralCurrencyFactory = mockk()
    private val userWallet: UserWallet = mockk()

    private val swaps = MutableStateFlow<List<SwapTransactionListModel>?>(null)
    private val status = MutableStateFlow<PredictionAccountStatusValue>(createActive(BigDecimal("1.00")))
    private var now = 1_000_000L

    private val window = DefaultPredictionFundingWindow(
        userWalletsListRepository = userWalletsListRepository,
        swapTransactionRepository = swapTransactionRepository,
        predictionAccountStatusSupplier = statusSupplier,
        collateralCurrencyFactory = collateralFactory,
        config = PredictionBalancePollingConfig(fundingWindow = 30.seconds),
        currentTimeMillis = { now },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository, swapTransactionRepository, statusSupplier, collateralFactory)
        swaps.value = null
        status.value = createActive(BigDecimal("1.00"))
        now = 1_000_000L
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { userWallet.walletId } returns WALLET_ID
        every { collateralFactory.createDepositTarget() } returns COLLATERAL
        every { COLLATERAL.id } returns COLLATERAL_ID
        every { swapTransactionRepository.getTransactions(userWallet, COLLATERAL_ID) } returns swaps
        every { statusSupplier(WALLET_ID) } returns status
    }

    @Test
    fun `GIVEN armed WHEN swap newer than arming appears THEN window opens`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()

            // Act
            window.arm(WALLET_ID)
            swaps.value = listOf(createSwapList(timestamp = now + 1))

            // Assert
            assertThat(awaitItem()).isTrue()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN armed WHEN only swaps older than arming exist THEN window stays closed`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()

            // Act
            swaps.value = listOf(createSwapList(timestamp = now - 1))
            window.arm(WALLET_ID)
            runCurrent()

            // Assert
            expectNoEvents()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN not armed WHEN swap appears THEN window stays closed`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()

            // Act
            swaps.value = listOf(createSwapList(timestamp = now + 1))
            runCurrent()

            // Assert
            expectNoEvents()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN open window WHEN funding window elapses THEN it closes`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            awaitItem()
            window.arm(WALLET_ID)
            swaps.value = listOf(createSwapList(timestamp = now + 1))
            assertThat(awaitItem()).isTrue()

            // Act
            advanceTimeBy(30.seconds)
            runCurrent()

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN open window WHEN balance changes THEN it closes early`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            awaitItem()
            window.arm(WALLET_ID)
            swaps.value = listOf(createSwapList(timestamp = now + 1))
            assertThat(awaitItem()).isTrue()

            // Act
            status.value = createActive(BigDecimal("3.00"))

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN window opened with unknown balance WHEN balance appears THEN closes only by timer`() = runTest {
        // Arrange
        status.value = PredictionAccountStatusValue.Loading
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            awaitItem()
            window.arm(WALLET_ID)
            swaps.value = listOf(createSwapList(timestamp = now + 1))
            assertThat(awaitItem()).isTrue()

            // Act
            status.value = createActive(BigDecimal("3.00"))
            runCurrent()
            expectNoEvents()
            advanceTimeBy(30.seconds)
            runCurrent()

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN window closed WHEN second swap arrives without arming THEN stays closed`() = runTest {
        // Arrange
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            awaitItem()
            window.arm(WALLET_ID)
            swaps.value = listOf(createSwapList(timestamp = now + 1))
            assertThat(awaitItem()).isTrue()
            advanceTimeBy(30.seconds)
            runCurrent()
            assertThat(awaitItem()).isFalse()

            // Act
            swaps.value = listOf(createSwapList(timestamp = now + 1), createSwapList(timestamp = now + 2))
            runCurrent()

            // Assert
            expectNoEvents()
        }
        watching.cancel()
    }

    private fun createSwapList(timestamp: Long): SwapTransactionListModel = mockk {
        every { transactions } returns listOf(mockk<SwapTransactionModel> { every { this@mockk.timestamp } returns timestamp })
    }

    private fun createActive(balance: BigDecimal) = PredictionAccountStatusValue.Active(
        source = StatusSource.ACTUAL,
        balance = balance,
        fiatRate = null,
        depositCurrency = COLLATERAL,
        depositAddress = null,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val COLLATERAL: CryptoCurrency.Token = mockk()
        val COLLATERAL_ID: CryptoCurrency.ID = mockk()
    }
}