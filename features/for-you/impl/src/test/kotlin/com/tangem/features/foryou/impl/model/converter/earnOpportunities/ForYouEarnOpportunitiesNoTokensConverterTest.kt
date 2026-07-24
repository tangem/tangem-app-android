package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import org.junit.jupiter.api.Test

internal class ForYouEarnOpportunitiesNoTokensConverterTest {

    @Test
    fun `GIVEN more top tokens than the cap WHEN convert THEN only first five are suggested`() {
        // Arrange
        val converter = ForYouEarnOpportunitiesNoTokensConverter(
            topEarnTokens = List(7) { index ->
                createTopEarnToken(tokenId = "token-$index", networkRawId = "NET")
            }.right(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(emptyList()) as EarnOpportunitiesUM.Content

        // Assert — suggestions live in a single header-less group
        assertThat(result.tokenList.map { it.header }).containsExactly(null)
        assertThat(result.items.map { it.tokenRowUM.id })
            .containsExactly("token-0-NET", "token-1-NET", "token-2-NET", "token-3-NET", "token-4-NET")
            .inOrder()
    }

    @Test
    fun `GIVEN top tokens WHEN convert THEN header shows first suggestion's rate and reward type`() {
        // Arrange
        val converter = ForYouEarnOpportunitiesNoTokensConverter(
            topEarnTokens = listOf(
                createTopEarnToken(apy = "7.25", rewardType = EarnRewardType.APR),
                createTopEarnToken(tokenId = "solana", apy = "99.9", rewardType = EarnRewardType.APY),
            ).right(),
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(emptyList()) as EarnOpportunitiesUM.Content

        // Assert — mirrors the production rate rendering for the first (best) suggestion
        val expectedRate = "7.25".parseBigDecimalOrNull().format { percent() }
        assertThat(result.potentialReward).isEqualTo(stringReference(expectedRate))
        assertThat(result.potentialRewardType).isEqualTo(stringReference("APR"))
        assertThat(result.subtitleRes).isEqualTo(R.string.for_you_earn_opportunities_no_available_tokens)
    }

    @Test
    fun `GIVEN no top tokens loaded WHEN convert THEN suggestions are empty and reward type is absent`() {
        // Arrange
        val converter = ForYouEarnOpportunitiesNoTokensConverter(
            topEarnTokens = null,
            onTokenClick = { _, _, _ -> },
            onAllEarnTokensClick = {},
        )

        // Act
        val result = converter.convert(emptyList()) as EarnOpportunitiesUM.Content

        // Assert
        assertThat(result.tokenList).isEmpty()
        assertThat(result.potentialRewardType).isNull()
    }
}