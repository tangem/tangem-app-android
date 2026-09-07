package com.tangem.features.tangempay.withdraw.hub.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayWithdrawHubUMFactoryTest {

    private val onBackClick: () -> Unit = mockk(relaxed = true)
    private val onWithinPortfolioClick: () -> Unit = mockk(relaxed = true)
    private val factory = TangemPayWithdrawHubUMFactory(
        onBackClick = onBackClick,
        onWithinPortfolioClick = onWithinPortfolioClick,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onBackClick, onWithinPortfolioClick)
    }

    @Test
    fun `GIVEN factory WHEN create THEN within portfolio card has hub title`() {
        // Act
        val actual = factory.create()

        // Assert
        assertThat(actual.swapSection.withinPortfolio.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_within_portfolio))
    }

    @Test
    fun `GIVEN created state WHEN within portfolio clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.swapSection.withinPortfolio.onClick()

        // Assert
        verify(exactly = 1) { onWithinPortfolioClick() }
    }

    @Test
    fun `GIVEN created state WHEN back clicked THEN callback invoked`() {
        // Arrange
        val state = factory.create()

        // Act
        state.onBackClick()

        // Assert
        verify(exactly = 1) { onBackClick() }
    }
}