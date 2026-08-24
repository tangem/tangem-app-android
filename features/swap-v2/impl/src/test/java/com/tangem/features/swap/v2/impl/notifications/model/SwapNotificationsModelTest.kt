package com.tangem.features.swap.v2.impl.notifications.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.notifications.DefaultSwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.entity.SwapNotificationUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Covers the swap-v2 half of the region-restriction gate: a restricted quote reaches this model as a
 * successful [SwapQuoteUM.Content] carrying `isRestricted = true` (never as an [ExpressError]), and it
 * must become a blocking [SwapNotificationUM.Error.RegionRestriction] so `hasErrorFlow` disables Send.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapNotificationsModelTest {

    private val updateListener: SwapNotificationsUpdateListener = mockk {
        every { updateTriggerFlow } returns emptyFlow()
    }
    private val updateTrigger: DefaultSwapNotificationsUpdateTrigger = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val fromCurrency = currencyFactory.createCoin(Blockchain.Ethereum)
    private val toCurrency = currencyFactory.createCoin(Blockchain.Bitcoin)

    @BeforeEach
    fun resetMocks() {
        clearMocks(updateTrigger, analyticsEventHandler)
    }

    @Test
    fun `GIVEN restricted quote WHEN model created THEN region restriction notification shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this, quote = contentQuote(isRestricted = true))

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).containsExactly(SwapNotificationUM.Error.RegionRestriction)
        coVerify { updateTrigger.callbackHasError(true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN purchasable quote WHEN model created THEN no notifications and no error reported`() = runTest {
        // Arrange
        val model = createModel(testScope = this, quote = contentQuote(isRestricted = false))

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEmpty()
        coVerify { updateTrigger.callbackHasError(false) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no quote WHEN model created THEN no notifications and no error reported`() = runTest {
        // Arrange
        val model = createModel(testScope = this, quote = null)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEmpty()
        coVerify { updateTrigger.callbackHasError(false) }
        model.onDestroy()
    }

    /**
     * Documents a guard-ordering hazard rather than a desired behaviour: `addExpressErrorNotification`
     * bails out unless BOTH currencies are known, and SendWithSwapConfirmModel supplies them from
     * nullable fields. When one is missing the express error is dropped, `hasError` stays false, and
     * nothing disables Send. If that is ever deemed unacceptable, this test is the one to flip.
     *
     * The restriction path is deliberately not subject to this guard — `maybeAddRegionRestrictionError`
     * reads only the quote, so it survives an unknown receive currency.
     */
    @Test
    fun `GIVEN express error and unknown receive currency WHEN model created THEN notification dropped`() = runTest {
        // Arrange
        val model = createModel(
            testScope = this,
            quote = SwapQuoteUM.Error(
                provider = provider,
                expressError = ExpressError.AmountError.TooSmallError(code = 1, amount = BigDecimal.ONE),
            ),
            toCryptoCurrency = null,
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEmpty()
        coVerify { updateTrigger.callbackHasError(false) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted quote WHEN model created THEN no analytics event sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this, quote = contentQuote(isRestricted = true))

        // Act
        advanceUntilIdle()

        // Assert — sendErrorAnalyticsIfNeeded has no branch for RegionRestriction, unlike sibling errors
        verify { analyticsEventHandler wasNot Called }
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        quote: SwapQuoteUM?,
        toCryptoCurrency: CryptoCurrency? = toCurrency,
    ): SwapNotificationsModel {
        val params = SwapNotificationsComponent.Params(
            swapNotificationData = SwapNotificationsComponent.Params.SwapNotificationData(
                quote = quote,
                fromCryptoCurrency = fromCurrency,
                // Empty address keeps the memo notification out of the way
                destinationAddress = "",
                toCryptoCurrencyStatus = toCryptoCurrency?.let {
                    CryptoCurrencyStatus(currency = it, value = mockk(relaxed = true))
                },
            ),
        )
        return SwapNotificationsModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            swapNotificationsUpdateListener = updateListener,
            swapNotificationsUpdateTrigger = updateTrigger,
            swapAmountUpdateTrigger = mockk(relaxed = true),
            isMemoRequiredUseCase = mockk(relaxed = true),
            analyticsEventHandler = analyticsEventHandler,
            paramsContainer = MutableParamsContainer(value = params),
        )
    }

    private fun contentQuote(isRestricted: Boolean): SwapQuoteUM.Content = SwapQuoteUM.Content(
        provider = provider,
        toAmount = BigDecimal.ONE,
        fromAmount = BigDecimal.ONE,
        toAmountValue = TextReference.EMPTY,
        fromAmountValue = TextReference.EMPTY,
        diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
        isSingleProvider = false,
        rate = TextReference.EMPTY,
        quoteId = null,
        isRestricted = isRestricted,
    )

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        val provider = ExpressProvider(
            providerId = "p1",
            name = "Test Provider",
            type = ExpressProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )
    }
}