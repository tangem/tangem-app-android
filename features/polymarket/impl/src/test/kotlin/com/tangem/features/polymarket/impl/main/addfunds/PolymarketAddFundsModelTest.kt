package com.tangem.features.polymarket.impl.main.addfunds

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.polymarket.impl.main.addfunds.analytics.PredictionAddFundsAnalyticsEvent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketAddFundsModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val callback: PolymarketAddFundsComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics, callback)
    }

    @Test
    fun `GIVEN no input at all WHEN model created THEN state is ready without any suspension`() {
        // Arrange
        val model = createModel()

        // Act
        val actual = model.uiState.items.map { it.iconRes }

        // Assert
        assertThat(actual).containsExactly(
            CoreUiR.drawable.ic_exchange_horizontal_24,
            CoreUiR.drawable.ic_arrow_down_24,
        ).inOrder()
    }

    @Test
    fun `GIVEN model created WHEN state read THEN rows carry the design copy`() {
        // Arrange
        val model = createModel()

        // Act
        val actual = model.uiState.items.map { it.title to it.description }

        // Assert
        assertThat(actual).containsExactly(
            resourceReference(R.string.common_swap) to
                resourceReference(R.string.prediction_add_funds_swap_subtitle),
            resourceReference(R.string.common_receive) to
                resourceReference(R.string.prediction_add_funds_receive_subtitle),
        ).inOrder()
    }

    @Test
    fun `GIVEN a funding sheet WHEN model created THEN screen opened is sent once`() {
        // Arrange & Act
        createModel()

        // Assert
        verify(exactly = 1) {
            analytics.send(match<PredictionAddFundsAnalyticsEvent> { it.event == "Method Screen Opened" })
        }
    }

    @Test
    fun `GIVEN a created model WHEN swap row tapped THEN callback called and event sent`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.items[0].onClick()

        // Assert
        verify(exactly = 1) { callback.onSwapClick() }
        verify(exactly = 1) {
            analytics.send(match<PredictionAddFundsAnalyticsEvent> { it.event == "Button - Swap" })
        }
    }

    @Test
    fun `GIVEN a created model WHEN receive row tapped THEN callback called and event sent`() {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.items[1].onClick()

        // Assert
        verify(exactly = 1) { callback.onReceiveClick() }
        verify(exactly = 1) {
            analytics.send(match<PredictionAddFundsAnalyticsEvent> { it.event == "Button - Receive" })
        }
    }

    private fun createModel() = PolymarketAddFundsModel(
        paramsContainer = MutableParamsContainer(PolymarketAddFundsComponent.Params(callback = callback)),
        dispatchers = TestingCoroutineDispatcherProvider(),
        analytics = analytics,
    )
}