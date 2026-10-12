package com.tangem.features.yield.supply.impl.active.model

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.usecase.GetYieldBoostStatusUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetDustMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Which sheet a tap on "Stop earning" opens while the module still holds claimable rewards. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyActiveModelClaimOfferTest {

    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase = mockk(relaxed = true)
    private val getYieldBoostStatusUseCase: GetYieldBoostStatusUseCase = mockk(relaxed = true)
    private val claimTracker: YieldSupplyClaimTracker = mockk()
    private val rewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk()
    private val tokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk()

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)
    private val network: Network = mockk(relaxed = true) {
        every { rawId } returns "ethereum"
        every { name } returns "Ethereum"
    }
    private val token: CryptoCurrency.Token = mockk(relaxed = true) {
        every { network } returns this@YieldSupplyActiveModelClaimOfferTest.network
        every { symbol } returns "USDT"
        every { decimals } returns 6
    }

    @BeforeEach
    fun setUp() {
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()
        every { singleAccountStatusListSupplier.invoke(userWalletId) } returns emptyFlow()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { yieldSupplyGetProtocolBalanceUseCase(any(), any()) } returns BigDecimal.ONE.right()
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
        every { claimTracker.outcomes } returns MutableSharedFlow()
        every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(false)
        coEvery { tokenStatusUseCase(any()) } returns MockYieldSupply.market().right()
        coEvery { rewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.claimableSummary().right()
    }

    @Test
    fun `GIVEN claimable rewards WHEN stop earning is tapped THEN the claim sheet is offered`() = runTest {
        // Arrange
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Claim)
        assertThat(model.wasClaimOfferedBeforeWithdrawal).isTrue()
    }

    @Test
    fun `GIVEN the claim was already offered WHEN stop earning is tapped again THEN the withdrawal opens`() =
        runTest {
            // Arrange
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()
            model.onStopEarning()
            advanceUntilIdle()

            // Act
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
        }

    @Test
    fun `GIVEN the backend defers the claim WHEN stop earning is tapped THEN the withdrawal opens`() = runTest {
        // Arrange
        coEvery { rewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.deferredSummary().right()
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
        assertThat(model.wasClaimOfferedBeforeWithdrawal).isFalse()
    }

    @Test
    fun `GIVEN a claim is already in flight WHEN stop earning is tapped THEN the withdrawal opens`() = runTest {
        // Arrange
        every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(true)
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
    }

    @Test
    fun `GIVEN the market is unknown WHEN stop earning is tapped THEN the withdrawal opens`() = runTest {
        // Arrange
        coEvery { tokenStatusUseCase(any()) } returns IllegalStateException("no market").left()
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
    }

    private companion object {
        const val USER_ADDRESS = "0xowner"
    }

    private class FakeSlotHost(navigation: SlotNavigation<YieldSupplyActiveRoute>) {

        var configuration: YieldSupplyActiveRoute? = null
            private set

        init {
            navigation.subscribe { event ->
                val old = configuration
                configuration = event.transformer(old)
                event.onComplete(configuration, old)
            }
        }
    }

    /** The address the offer is keyed by arrives with the balance, so the status has to carry one. */
    private fun TestScope.createModelWithAddress(): YieldSupplyActiveModel = createModel().also { model ->
        model.cryptoCurrencyStatusFlow.value = mockk(relaxed = true) {
            every { currency } returns token
            every { value.networkAddress?.defaultAddress?.value } returns USER_ADDRESS
        }
    }

    private fun TestScope.createModel(): YieldSupplyActiveModel = YieldSupplyActiveModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyActiveComponent.Params(userWalletId = userWalletId, cryptoCurrency = token),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analyticsHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        yieldSupplyGetProtocolBalanceUseCase = yieldSupplyGetProtocolBalanceUseCase,
        yieldSupplyGetTokenStatusUseCase = tokenStatusUseCase,
        getYieldRewardsSummaryUseCase = rewardsSummaryUseCase,
        yieldSupplyMinAmountUseCase = mockk<YieldSupplyMinAmountUseCase>(relaxed = true),
        yieldSupplyGetCurrentFeeUseCase = mockk<YieldSupplyGetCurrentFeeUseCase>(relaxed = true),
        yieldSupplyGetMaxFeeUseCase = mockk<YieldSupplyGetMaxFeeUseCase>(relaxed = true),
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        urlOpener = mockk<UrlOpener>(relaxed = true),
        appRouter = mockk<AppRouter>(relaxed = true),
        yieldSupplyGetDustMinAmountUseCase = mockk<YieldSupplyGetDustMinAmountUseCase>(relaxed = true),
        getYieldBoostStatusUseCase = getYieldBoostStatusUseCase,
        boostStoryPreloader = mockk<YieldBoostStoryPreloader>(relaxed = true),
        yieldSupplyClaimTracker = claimTracker,
    )
}