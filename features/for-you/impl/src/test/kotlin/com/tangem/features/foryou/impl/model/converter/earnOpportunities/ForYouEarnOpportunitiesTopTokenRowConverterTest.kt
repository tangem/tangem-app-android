package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.earn.EarnType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouEarnOpportunitiesTopTokenRowConverterTest {

    private val converter = ForYouEarnOpportunitiesTopTokenRowConverter()

    @Test
    fun `GIVEN top-earn token WHEN convert THEN row carries currency identity and network subtitle`() {
        // Arrange
        val topToken = createTopEarnToken(
            tokenId = "solana",
            networkRawId = "SOL",
            networkName = "Solana",
            name = "Solana",
            apy = "7.25",
        )

        // Act
        val result = converter.convert(topToken)

        // Assert
        val row = result.tokenRowUM as TangemTokenRowUM.Content
        assertThat(row.id).isEqualTo("solana-SOL")
        assertThat(row.titleUM).isEqualTo(TangemTokenRowUM.TitleUM.Content(text = stringReference("Solana")))
        assertThat(row.subtitleUM).isEqualTo(
            TangemTokenRowUM.SubtitleUM.Content(
                text = resourceReference(R.string.wallet_network_group_title, wrappedList("Solana")),
            ),
        )
        assertThat(row.topEndContentUM).isEqualTo(
            TangemTokenRowUM.EndContentUM.Content(
                text = resourceReference(R.string.markets_apy_placeholder, wrappedList("7.25".expectedPercent())),
            ),
        )
    }

    @Test
    fun `GIVEN staking token WHEN convert THEN bottom end labels staking`() {
        // Arrange
        val topToken = createTopEarnToken(type = EarnType.STAKING)

        // Act
        val result = converter.convert(topToken)

        // Assert
        val row = result.tokenRowUM as TangemTokenRowUM.Content
        assertThat(row.bottomEndContentUM).isEqualTo(
            TangemTokenRowUM.EndContentUM.Content(text = resourceReference(R.string.common_staking)),
        )
    }

    @Test
    fun `GIVEN yield token WHEN convert THEN bottom end labels yield mode`() {
        // Arrange
        val topToken = createTopEarnToken(type = EarnType.YIELD)

        // Act
        val result = converter.convert(topToken)

        // Assert
        val row = result.tokenRowUM as TangemTokenRowUM.Content
        assertThat(row.bottomEndContentUM).isEqualTo(
            TangemTokenRowUM.EndContentUM.Content(text = resourceReference(R.string.common_yield_mode)),
        )
    }

    @Test
    fun `GIVEN top-earn token WHEN convert THEN item is a flat non-expandable row`() {
        // Act
        val result = converter.convert(createTopEarnToken())

        // Assert
        assertThat(result.isExpandable).isFalse()
        assertThat(result.isExpanded).isFalse()
        assertThat(result.tokenList).isEmpty()
    }

    /** Mirrors the production APY rendering used by [ForYouEarnOpportunitiesTopTokenRowConverter]. */
    private fun String.expectedPercent(): TextReference =
        TextReference.Str(BigDecimal(this).format { percent(withPercentSign = false) })
}