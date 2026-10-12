package com.tangem.features.feed.components.v2.blocks.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeedBlocksAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val shtorkaExpansion = MutableStateFlow(ShtorkaExpansion.FULL)

    private val analytics = FeedBlocksAnalytics(
        analyticsEventHandler = analyticsEventHandler,
        shtorkaExpansion = shtorkaExpansion,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
        shtorkaExpansion.value = ShtorkaExpansion.FULL
    }

    @ParameterizedTest
    @ProvideTestModels
    fun shtorkaState(model: ShtorkaStateModel) {
        // Arrange
        shtorkaExpansion.value = model.expansion

        // Act
        analytics.onForYouClicked()

        // Assert
        assertThat(capturedEvent().params).containsExactly("Shtorka State", model.expectedState)
    }

    private fun provideTestModels() = listOf(
        ShtorkaStateModel(expansion = ShtorkaExpansion.PILL, expectedState = "Collapsed"),
        ShtorkaStateModel(expansion = ShtorkaExpansion.PARTIAL, expectedState = "Half"),
        ShtorkaStateModel(expansion = ShtorkaExpansion.FULL, expectedState = "Full"),
    )

    internal data class ShtorkaStateModel(val expansion: ShtorkaExpansion, val expectedState: String)

    @Test
    fun `GIVEN half open shtorka WHEN product card shown THEN card and state reported`() {
        // Arrange
        shtorkaExpansion.value = ShtorkaExpansion.PARTIAL

        // Act
        analytics.onProductCardShown(ProductCard.ReferAFriend)

        // Assert
        val event = capturedEvent()
        assertThat(event.category).isEqualTo("Markets")
        assertThat(event.event).isEqualTo("Product Card Shown")
        assertThat(event.params).containsExactly(
            "Product Card", "Refer a Friend",
            "Shtorka State", "Half",
        )
    }

    @Test
    fun `GIVEN full shtorka WHEN product card clicked THEN card and state reported`() {
        // Act
        analytics.onProductCardClicked(ProductCard.TangemPay)

        // Assert
        val event = capturedEvent()
        assertThat(event.event).isEqualTo("Product Card Clicked")
        assertThat(event.params).containsExactly(
            "Product Card", "Tangem Pay",
            "Shtorka State", "Full",
        )
    }

    @Test
    fun `GIVEN full shtorka WHEN for you clicked THEN only state reported`() {
        // Act
        analytics.onForYouClicked()

        // Assert
        val event = capturedEvent()
        assertThat(event.event).isEqualTo("For You Clicked")
        assertThat(event.params).containsExactly("Shtorka State", "Full")
    }

    @Test
    fun `GIVEN full shtorka WHEN add account clicked THEN markets source reported`() {
        // Act
        analytics.onAddAccountClicked()

        // Assert
        val event = capturedEvent()
        assertThat(event.category).isEqualTo("Settings / Wallet Settings")
        assertThat(event.event).isEqualTo("Button - Add Account")
        assertThat(event.params).containsExactly(
            AnalyticsParam.SOURCE, "Markets",
            "Shtorka State", "Full",
        )
    }

    @Test
    fun `GIVEN two cards WHEN shown THEN each carries its own one-time id`() {
        // Act
        analytics.onProductCardShown(ProductCard.Earn)
        analytics.onProductCardShown(ProductCard.TangemPay)

        // Assert
        val ids = capturedEvents().filterIsInstance<FeedBlocksAnalyticsEvent.ProductCardShown>()
            .map { it.oneTimeEventId }
        assertThat(ids).containsExactly("Product Card Shown Earn", "Product Card Shown Tangem Pay")
    }

    private fun capturedEvent(): AnalyticsEvent = capturedEvents().single()

    private fun capturedEvents(): List<AnalyticsEvent> {
        val events = mutableListOf<AnalyticsEvent>()
        verify { analyticsEventHandler.send(capture(events)) }

        return events
    }
}