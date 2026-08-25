package com.tangem.features.feed.earn.model.state.transformers

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.features.feed.earn.createEarnCurrency
import com.tangem.features.feed.earn.createEarnHttpError
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

/**
 * The Mostly used carousel is ranked by yield, so the order of the produced ids is the assertion that
 * matters. Whole-object comparison is impossible here: every [com.tangem.features.feed.earn.ui.state
 * .EarnOpportunitiesItemUM] carries a click lambda and a styled APY reference whose span-style lambda
 * never compares equal.
 */
internal class UpdateOpportunitiesStateTransformerTest {

    private val onRetryClick: () -> Unit = {}
    private var clicked: Pair<EarnTokenWithCurrency, EarnSource>? = null

    private val prevState = EarnFeedTabUM(
        mostlyUsed = EarnListUM.Loading,
        bestOpportunities = EarnBestOpportunitiesUM.Empty,
        filters = persistentListOf<TangemFilterItemUM>(TangemFilterItemUM.Loading(id = "network")),
        onSliderScroll = {},
    )

    private fun transform(earnResult: EarnTopToken?): EarnFeedTabUM =
        UpdateOpportunitiesStateTransformer(
            earnResult = earnResult,
            onItemClick = { token, source -> clicked = token to source },
            onRetryClick = onRetryClick,
        ).transform(prevState)

    @Test
    fun `GIVEN the tokens have not arrived WHEN transformed THEN the carousel shimmers`() {
        // Act
        val actual = transform(earnResult = null)

        // Assert
        assertThat(actual).isEqualTo(prevState.copy(mostlyUsed = EarnListUM.Loading))
    }

    @Test
    fun `GIVEN the request failed WHEN transformed THEN the carousel offers a retry`() {
        // Act
        val actual = transform(earnResult = createEarnHttpError().left())

        // Assert — the expected instance carries the very lambda that was handed to the transformer
        assertThat(actual).isEqualTo(prevState.copy(mostlyUsed = EarnListUM.Error(onRetryClicked = onRetryClick)))
    }

    @Test
    fun `GIVEN tokens of different yields WHEN transformed THEN the richest one comes first`() {
        // Arrange
        val tokens = listOf(
            earnTokenWithCurrency(id = "low", apy = "0.01", tokenName = "Low"),
            earnTokenWithCurrency(id = "high", apy = "0.10", tokenName = "High"),
            earnTokenWithCurrency(id = "mid", apy = "0.05", tokenName = "Mid"),
        )

        // Act
        val actual = transform(earnResult = tokens.right())

        // Assert
        assertThat(actual.mostlyUsed.itemIds()).containsExactly("high_STAKING", "mid_STAKING", "low_STAKING")
            .inOrder()
    }

    @Test
    fun `GIVEN tokens of equal yield WHEN transformed THEN they are ordered by name`() {
        // Arrange
        val tokens = listOf(
            earnTokenWithCurrency(id = "beta", apy = "0.05", tokenName = "Beta"),
            earnTokenWithCurrency(id = "alpha", apy = "0.05", tokenName = "Alpha"),
        )

        // Act
        val actual = transform(earnResult = tokens.right())

        // Assert
        assertThat(actual.mostlyUsed.itemIds()).containsExactly("alpha_STAKING", "beta_STAKING").inOrder()
    }

    @Test
    fun `GIVEN a successful but empty list WHEN transformed THEN the carousel holds no items`() {
        // Act — note that EarnListUM.Empty is never produced; an empty success reads as empty content
        val actual = transform(earnResult = emptyList<EarnTokenWithCurrency>().right())

        // Assert
        assertThat(actual.mostlyUsed).isInstanceOf(EarnListUM.Content::class.java)
        assertThat(actual.mostlyUsed.itemIds()).isEmpty()
    }

    @Test
    fun `GIVEN tokens WHEN transformed THEN the rest of the tab is left untouched`() {
        // Act
        val actual = transform(earnResult = listOf(earnTokenWithCurrency()).right())

        // Assert
        assertThat(actual.bestOpportunities).isEqualTo(prevState.bestOpportunities)
        assertThat(actual.filters).isEqualTo(prevState.filters)
        assertThat(actual.onSliderScroll).isEqualTo(prevState.onSliderScroll)
    }

    @Test
    fun `GIVEN a carousel item WHEN it is clicked THEN the click is reported as coming from Mostly used`() {
        // Arrange
        val token = earnTokenWithCurrency()

        // Act
        val actual = transform(earnResult = listOf(token).right())
        (actual.mostlyUsed as EarnListUM.Content).items.single().onItemClick()

        // Assert
        assertThat(clicked).isEqualTo(token to EarnSource.MOSTLY_USED_SOURCE)
    }

    private fun earnTokenWithCurrency(
        id: String = "coin-ethereum",
        apy: String = "0.05",
        tokenName: String = "Ethereum",
    ): EarnTokenWithCurrency = createEarnTokenWithCurrency(
        earnToken = createEarnToken(apy = apy, tokenName = tokenName),
        cryptoCurrency = createEarnCurrency(currencyId = id),
    )

    private fun EarnListUM.itemIds(): List<String> = (this as EarnListUM.Content).items.map { it.id }
}