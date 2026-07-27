package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.model.SwapHoldingsState
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BottomButtonUMConverterTest {

    private var addFundsClicks = 0
    private val swapClicks = mutableListOf<List<TokenSelectorEntry>>()

    private val converter = BottomButtonUMConverter(
        onAddFundsClick = { addFundsClicks++ },
        onSwapClick = swapClicks::add,
    )

    @BeforeEach
    fun resetClicks() {
        addFundsClicks = 0
        swapClicks.clear()
    }

    @Test
    fun `GIVEN holdings are being resolved WHEN converted THEN the button shimmers`() {
        // Act
        val actual = converter.convert(SwapHoldingsState.Loading)

        // Assert
        assertThat(actual).isEqualTo(BottomButtonUM.Loading)
    }

    @Test
    fun `GIVEN no holding has a balance WHEN the button is clicked THEN add funds is opened`() {
        // Act
        val actual = converter.convert(SwapHoldingsState.ZeroBalance) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.common_add_funds))
        assertThat(actual.isEnabled).isTrue()

        actual.onClick()
        assertThat(addFundsClicks).isEqualTo(1)
        assertThat(swapClicks).isEmpty()
    }

    @Test
    fun `GIVEN swappable holdings WHEN the button is clicked THEN they are handed over to the swap flow`() {
        // Arrange
        val entries = listOf<TokenSelectorEntry>(mockk())

        // Act
        val actual = converter.convert(SwapHoldingsState.Available(entries)) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.token_summary_go_to_swap_button))
        assertThat(actual.isEnabled).isTrue()

        actual.onClick()
        assertThat(swapClicks).containsExactly(entries)
        assertThat(addFundsClicks).isEqualTo(0)
    }

    @Test
    fun `GIVEN no holding can be swapped from WHEN converted THEN swap is disabled and does nothing`() {
        // Act
        val actual = converter.convert(SwapHoldingsState.Unavailable) as BottomButtonUM.Content

        // Assert
        assertThat(actual.text).isEqualTo(resourceReference(R.string.token_summary_go_to_swap_button))
        assertThat(actual.isEnabled).isFalse()

        actual.onClick()
        assertThat(swapClicks).isEmpty()
        assertThat(addFundsClicks).isEqualTo(0)
    }
}