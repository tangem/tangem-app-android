package com.tangem.features.yield.supply.impl.active.model

import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.promo.usecase.GetYieldBoostStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetDustMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyActiveModelClaimOutcomeTest {

    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getYieldBoostStatusUseCase: GetYieldBoostStatusUseCase = mockk(relaxed = true)
    private val claimTracker: YieldSupplyClaimTracker = mockk()
    private val outcomes = MutableSharedFlow<YieldSupplyClaimOutcome>(extraBufferCapacity = 1)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)
    private val network: Network = mockk(relaxed = true) {
        every { rawId } returns "ethereum"
        every { name } returns "Ethereum"
    }
    private val token: CryptoCurrency.Token = mockk(relaxed = true) {
        every { id } returns CRYPTO_CURRENCY_ID
        every { network } returns this@YieldSupplyActiveModelClaimOutcomeTest.network
        every { symbol } returns "USDT"
        every { decimals } returns 6
    }

    @BeforeEach
    fun setUp() {
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()
        every { singleAccountStatusListSupplier.invoke(userWalletId) } returns emptyFlow()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { yieldSupplyGetProtocolBalanceUseCase(any(), any()) } returns BigDecimal.ONE.right()
        every { claimTracker.outcomes } returns outcomes
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
    }

    @Test
    fun `GIVEN a submitted claim WHEN it is confirmed THEN the result turns into a confirmed one`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Confirmed))
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value?.status)
            .isInstanceOf(YieldSupplyClaimResultUM.Status.Confirmed::class.java)
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.ClaimResult)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a submitted claim WHEN it reverts THEN the result sheet is closed`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value).isNull()
        assertThat(slot.configuration).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a claim of another token WHEN it is confirmed THEN this result is left alone`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        outcomes.emit(
            outcome(YieldSupplyTransactionOutcome.Confirmed).copy(
                cryptoCurrencyId = CryptoCurrency.ID(
                    prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                    body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                    suffix = CryptoCurrency.ID.Suffix.RawID("0xother"),
                ),
            ),
        )
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value?.status).isEqualTo(YieldSupplyClaimResultUM.Status.Submitted)

        model.onDestroy()
    }

    @Test
    fun `GIVEN another sheet open WHEN a claim reverts THEN that sheet is left alone`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onStopEarning()

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a sheet opened over the result WHEN the claim reverts THEN that sheet is left alone`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())
        model.onApprove()

        // Act
        outcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Approve)
        assertThat(model.claimResultFlow.value).isNull()

        model.onDestroy()
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

    private fun submittedResult() = YieldSupplyClaimResultUM(
        status = YieldSupplyClaimResultUM.Status.Submitted,
        total = stringReference("$45.50"),
        serviceFee = stringReference("$1.40"),
        networkFee = stringReference("$1.40"),
    )

    private fun outcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
        userWalletId = userWalletId,
        cryptoCurrencyId = CRYPTO_CURRENCY_ID,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        outcome = outcome,
    )

    private fun TestScope.createModel(): YieldSupplyActiveModel = YieldSupplyActiveModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyActiveComponent.Params(userWalletId = userWalletId, cryptoCurrency = token),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analyticsHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        yieldSupplyGetProtocolBalanceUseCase = yieldSupplyGetProtocolBalanceUseCase,
        yieldSupplyGetTokenStatusUseCase = mockk<YieldSupplyGetTokenStatusUseCase>(relaxed = true),
        yieldSupplyMinAmountUseCase = mockk<YieldSupplyMinAmountUseCase>(relaxed = true),
        yieldSupplyGetCurrentFeeUseCase = mockk<YieldSupplyGetCurrentFeeUseCase>(relaxed = true),
        yieldSupplyGetMaxFeeUseCase = mockk<YieldSupplyGetMaxFeeUseCase>(relaxed = true),
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        urlOpener = mockk<UrlOpener>(relaxed = true),
        appRouter = appRouter,
        yieldSupplyGetDustMinAmountUseCase = mockk<YieldSupplyGetDustMinAmountUseCase>(relaxed = true),
        getYieldBoostStatusUseCase = getYieldBoostStatusUseCase,
        boostStoryPreloader = mockk<YieldBoostStoryPreloader>(relaxed = true),
        yieldSupplyClaimTracker = claimTracker,
    )

    private companion object {
        const val CHAIN_ID = 1
        const val USER_ADDRESS = "0xowner"
        val CRYPTO_CURRENCY_ID = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdt"),
        )
    }
}