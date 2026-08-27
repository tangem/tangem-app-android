package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.features.foryou.impl.tokensummary.model.SwapHoldingsState
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BottomButtonUMConverterTest {

    private var addToPortfolioClicks = 0
    private var addFundsClicks = 0
    private val swapClicks = mutableListOf<List<SwapHolding>>()

    @BeforeEach
    fun resetClicks() {
        addToPortfolioClicks = 0
        addFundsClicks = 0
        swapClicks.clear()
    }

    @Test
    fun `GIVEN holdings are being resolved WHEN converted THEN the button shimmers`() {
        // Act
        val actual = createConverter().convert(SwapHoldingsState.Loading)

        // Assert
        assertThat(actual).isEqualTo(BottomButtonUM.Loading)
    }

    @Test
    fun `GIVEN the token is not held WHEN the button is clicked THEN adding it to a portfolio is offered`() {
        // Act
        val actual = createConverter().convert(SwapHoldingsState.NotHeld) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.common_add_funds))
        assertThat(actual.isEnabled).isTrue()

        actual.onClick()
        assertThat(addToPortfolioClicks).isEqualTo(1)
        assertThat(addFundsClicks).isEqualTo(0)
        assertThat(swapClicks).isEmpty()
    }

    @Test
    fun `GIVEN the token cannot be added WHEN it is not held THEN swap is disabled instead`() {
        // Arrange — a token whose networks the caller never resolved has nothing to offer
        val converter = createConverter(isAddToPortfolioAvailable = false)

        // Act
        val actual = converter.convert(SwapHoldingsState.NotHeld) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.token_summary_go_to_swap_button))
        assertThat(actual.isEnabled).isFalse()

        actual.onClick()
        assertThat(addToPortfolioClicks).isEqualTo(0)
    }

    @Test
    fun `GIVEN no holding has a balance WHEN the button is clicked THEN add funds is opened`() {
        // Act
        val actual = createConverter().convert(SwapHoldingsState.ZeroBalance) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.common_add_funds))
        assertThat(actual.isEnabled).isTrue()

        actual.onClick()
        assertThat(addFundsClicks).isEqualTo(1)
        assertThat(addToPortfolioClicks).isEqualTo(0)
        assertThat(swapClicks).isEmpty()
    }

    @Test
    fun `GIVEN holdings with funds WHEN the button is clicked THEN they are handed over to the swap flow`() {
        // Arrange — an unavailable holding is handed over too: it explains itself once picked
        val holdings = listOf(
            holding(ScenarioUnavailabilityReason.None),
            holding(ScenarioUnavailabilityReason.SingleWallet),
        )

        // Act
        val actual = createConverter().convert(SwapHoldingsState.Resolved(holdings)) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.token_summary_go_to_swap_button))
        assertThat(actual.isEnabled).isTrue()

        actual.onClick()
        assertThat(swapClicks).containsExactly(holdings)
        assertThat(addFundsClicks).isEqualTo(0)
        assertThat(addToPortfolioClicks).isEqualTo(0)
    }

    @Test
    fun `GIVEN the token has no market identity WHEN converted THEN swap is disabled and does nothing`() {
        // Act
        val actual = createConverter().convert(SwapHoldingsState.Unavailable) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.token_summary_go_to_swap_button))
        assertThat(actual.isEnabled).isFalse()

        actual.onClick()
        assertThat(swapClicks).isEmpty()
        assertThat(addFundsClicks).isEqualTo(0)
        assertThat(addToPortfolioClicks).isEqualTo(0)
    }

    private fun createConverter(isAddToPortfolioAvailable: Boolean = true) = BottomButtonUMConverter(
        isAddToPortfolioAvailable = isAddToPortfolioAvailable,
        onAddToPortfolioClick = { addToPortfolioClicks++ },
        onAddFundsClick = { addFundsClicks++ },
        onSwapClick = swapClicks::add,
    )

    private fun holding(reason: ScenarioUnavailabilityReason) =
        SwapHolding(entry = mockk(), unavailabilityReason = reason)
}