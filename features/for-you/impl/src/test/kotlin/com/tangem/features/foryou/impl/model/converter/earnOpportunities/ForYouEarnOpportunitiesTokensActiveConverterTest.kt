package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.domain.models.earn.EarnError
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class ForYouEarnOpportunitiesTokensActiveConverterTest {

    @Test
    fun `GIVEN top tokens contain active portfolio assets WHEN convert THEN active ones are excluded`() {
        // Arrange
        val activePortfolio = createEarnOpportunities(
            earnCurrencies = mapOf(
                createStatus(createEarnCurrency(tokenId = "ethereum", networkRawId = "ETH")) to createEarnApyInfo(),
            ),
        )
        val converter = ForYouEarnOpportunitiesTokensActiveConverter(
            topEarnTokens = listOf(
                createTopEarnToken(tokenId = "ethereum", networkRawId = "ETH"),
                createTopEarnToken(tokenId = "solana", networkRawId = "SOL"),
            ).right(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(listOf(activePortfolio)) as EarnOpportunitiesUM.Content

        // Assert
        assertThat(result.items.map { it.tokenRowUM.id }).containsExactly("solana-SOL")
    }

    @Test
    fun `GIVEN more suggestions than the cap WHEN convert THEN filtering happens before the top-5 cut`() {
        // Arrange — two of the first candidates are active; the cap must still be filled from the tail
        val activePortfolio = createEarnOpportunities(
            earnCurrencies = listOf("token-0", "token-1").associate { tokenId ->
                createStatus(createEarnCurrency(tokenId = tokenId, networkRawId = "NET")) to createEarnApyInfo()
            },
        )
        val converter = ForYouEarnOpportunitiesTokensActiveConverter(
            topEarnTokens = List(8) { index ->
                createTopEarnToken(tokenId = "token-$index", networkRawId = "NET")
            }.right(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(listOf(activePortfolio)) as EarnOpportunitiesUM.Content

        // Assert
        assertThat(result.items.map { it.tokenRowUM.id })
            .containsExactly("token-2-NET", "token-3-NET", "token-4-NET", "token-5-NET", "token-6-NET")
            .inOrder()
    }

    @Test
    fun `GIVEN asset active on another network WHEN convert THEN suggestion on a new network is kept`() {
        // Arrange — matching is per asset AND network, not per asset
        val activePortfolio = createEarnOpportunities(
            earnCurrencies = mapOf(
                createStatus(createEarnCurrency(tokenId = "usd-coin", networkRawId = "ETH")) to createEarnApyInfo(),
            ),
        )
        val converter = ForYouEarnOpportunitiesTokensActiveConverter(
            topEarnTokens = listOf(
                createTopEarnToken(tokenId = "usd-coin", networkRawId = "ETH"),
                createTopEarnToken(tokenId = "usd-coin", networkRawId = "SOL"),
            ).right(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(listOf(activePortfolio)) as EarnOpportunitiesUM.Content

        // Assert
        assertThat(result.items.map { it.tokenRowUM.id }).containsExactly("usd-coin-SOL")
    }

    @Test
    fun `GIVEN no top tokens loaded WHEN convert THEN content with empty suggestions`() {
        // Arrange — the callback instance is shared with `expected` so the whole-object equality holds
        val onAllEarnTokensClick: () -> Unit = {}
        val converter = ForYouEarnOpportunitiesTokensActiveConverter(
            topEarnTokens = null,
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = onAllEarnTokensClick,
        )

        // Act
        val result = converter.convert(listOf(createEarnOpportunities()))

        // Assert
        val expected = EarnOpportunitiesUM.Content(
            tokenList = persistentListOf(),
            subtitleRes = R.string.for_you_earn_opportunities_all_tokens_active,
            potentialReward = null,
            potentialRewardType = null,
            onAllEarnTokensClick = onAllEarnTokensClick,
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN top tokens failed to load WHEN convert THEN suggestions are empty`() {
        // Arrange
        val converter = ForYouEarnOpportunitiesTokensActiveConverter(
            topEarnTokens = EarnError.NotHttpError().left(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(listOf(createEarnOpportunities())) as EarnOpportunitiesUM.Content

        // Assert
        assertThat(result.tokenList).isEmpty()
    }
}