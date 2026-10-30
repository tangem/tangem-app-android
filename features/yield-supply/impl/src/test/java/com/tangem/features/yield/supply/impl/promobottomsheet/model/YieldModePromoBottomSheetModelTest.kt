package com.tangem.features.yield.supply.impl.promobottomsheet.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics.CloseMethod
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoBottomSheetComponent
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.formatter.formatApyValue
import com.tangem.features.yield.supply.impl.promobottomsheet.entity.YieldModePromoBottomSheetUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModePromoBottomSheetModelTest {

    private val analytics: AnalyticsEventHandler = mockk()
    private val router: Router = mockk(relaxed = true)
    private val yieldSupplyRepository: YieldSupplyRepository = mockk()

    private val sentEvents = mutableListOf<AnalyticsEvent>()
    private var dismissCount = 0

    private val offer = YieldModePromoOffer(
        apy = BigDecimal("9.20"),
        tokensCount = 36,
        tokenSymbol = "USDT",
        blockchain = "Ethereum",
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics, router, yieldSupplyRepository)
        sentEvents.clear()
        dismissCount = 0
        every { analytics.send(capture(sentEvents)) } just runs
        coEvery { yieldSupplyRepository.setYieldModePromoBottomSheetShown() } just runs
    }

    @Test
    fun `GIVEN an offer WHEN model created THEN the sheet shows its APY, tokens count and the asset icons`() = runTest {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.title).isEqualTo(
            resourceReference(R.string.yield_module_main_promo_title, wrappedList(offer.apy.formatApyValue())),
        )
        assertThat(state.tokensCount).isEqualTo(
            pluralReference(R.plurals.yield_module_main_promo_tokens_count, count = 36, formatArgs = wrappedList(36)),
        )
        assertThat(state.tokenIconUrls).containsExactly(
            "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
            "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/usd-coin.png",
            "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/dai.png",
        ).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the sheet drawn twice WHEN shown THEN opened is sent and the flag is written once`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onShown()
        model.uiState.value.onShown()
        advanceUntilIdle()

        // Assert
        assertThat(sentEvents).containsExactly(YieldSupplyAnalytics.PromoScreenOpened(offer))
        coVerify(exactly = 1) { yieldSupplyRepository.setYieldModePromoBottomSheetShown() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the sheet shown WHEN the model is destroyed before the write runs THEN the flag is still written`() =
        runTest {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onShown()
            model.onDestroy()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { yieldSupplyRepository.setYieldModePromoBottomSheetShown() }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an open sheet WHEN the user closes it THEN closed is sent with the method`(model: CloseModel) = runTest {
        // Arrange
        val sheetModel = createModel()

        // Act
        model.close(sheetModel.uiState.value)

        // Assert
        assertThat(sentEvents).containsExactly(YieldSupplyAnalytics.PromoScreenClosed(offer, model.expected))
        assertThat(dismissCount).isEqualTo(1)
        sheetModel.onDestroy()
    }

    @Test
    fun `GIVEN an open sheet WHEN try is tapped twice THEN Earn opens on Yield once`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onTryClick()
        model.uiState.value.onTryClick()

        // Assert
        assertThat(sentEvents).containsExactly(YieldSupplyAnalytics.ButtonTryYieldMode(offer))
        assertThat(dismissCount).isEqualTo(1)
        verify(exactly = 1) { router.push(AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Yield), any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideFinishModels")
    fun `GIVEN a finished sheet WHEN it is closed or tapped again THEN nothing happens`(model: FinishModel) = runTest {
        // Arrange
        val sheetModel = createModel()
        val state = sheetModel.uiState.value
        model.finish(sheetModel)
        sentEvents.clear()
        dismissCount = 0

        // Act
        state.onCloseClick()
        state.onBack()
        state.onDismissRequest()
        state.onTryClick()

        // Assert
        assertThat(sentEvents).isEmpty()
        assertThat(dismissCount).isEqualTo(0)
        verify(exactly = model.expectedPushes) { router.push(any(), any()) }
        sheetModel.onDestroy()
    }

    @Test
    fun `GIVEN an open sheet WHEN the slot dismisses it THEN no closed event is sent`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.dismiss()

        // Assert
        assertThat(sentEvents).isEmpty()
        assertThat(dismissCount).isEqualTo(1)
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideParamsModels")
    fun `GIVEN an offer WHEN an event is sent THEN it carries the offer params`(model: ParamsModel) = runTest {
        // Arrange
        val sheetModel = createModel()

        // Act
        model.action(sheetModel.uiState.value)

        // Assert
        assertThat(sentEvents.map { it.params }).containsExactly(model.expected)
        sheetModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        CloseModel(close = { it.onCloseClick() }, expected = CloseMethod.Cross),
        CloseModel(close = { it.onBack() }, expected = CloseMethod.SystemBack),
        CloseModel(close = { it.onDismissRequest() }, expected = CloseMethod.Swipe),
    )

    private fun provideFinishModels() = listOf(
        FinishModel(finish = { it.uiState.value.onCloseClick() }, expectedPushes = 0),
        FinishModel(finish = { it.uiState.value.onTryClick() }, expectedPushes = 1),
        FinishModel(finish = { it.onDestroy() }, expectedPushes = 0),
    )

    private fun provideParamsModels() = listOf(
        ParamsModel(
            action = { it.onShown() },
            expected = mapOf("Token" to "USDT", "Blockchain" to "Ethereum", "APY" to "9.20", "Tokens Count" to "36"),
        ),
        ParamsModel(
            action = { it.onCloseClick() },
            expected = mapOf("Token" to "USDT", "Blockchain" to "Ethereum", "APY" to "9.20", "Close Method" to "X"),
        ),
        ParamsModel(
            action = { it.onTryClick() },
            expected = mapOf("Token" to "USDT", "Blockchain" to "Ethereum", "APY" to "9.20"),
        ),
    )

    internal data class CloseModel(val close: (YieldModePromoBottomSheetUM) -> Unit, val expected: CloseMethod)

    internal data class FinishModel(val finish: (YieldModePromoBottomSheetModel) -> Unit, val expectedPushes: Int)

    internal data class ParamsModel(
        val action: (YieldModePromoBottomSheetUM) -> Unit,
        val expected: Map<String, String>,
    )

    private fun TestScope.createModel() = YieldModePromoBottomSheetModel(
        paramsContainer = MutableParamsContainer(
            YieldModePromoBottomSheetComponent.Params(offer = offer, onDismiss = { dismissCount++ }),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        analytics = analytics,
        yieldSupplyRepository = yieldSupplyRepository,
        appCoroutineScope = TestAppCoroutineScope(this),
    )
}