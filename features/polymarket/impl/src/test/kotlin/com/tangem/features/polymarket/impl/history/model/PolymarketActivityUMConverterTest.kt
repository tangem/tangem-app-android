package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM
import com.tangem.utils.StringsSigns.MINUS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

internal class PolymarketActivityUMConverterTest {

    private val previousLocale = Locale.getDefault()
    private val clock = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.ofHours(3))

    @BeforeEach
    fun setLocale() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `GIVEN operations around local midnight WHEN grouped THEN dates and newest first order use the local zone`() {
        // Arrange
        val items = listOf(
            createHistoryActivity(id = "yesterday", occurredAt = "2026-09-27T20:59:00Z"),
            createHistoryActivity(id = "midnight", occurredAt = "2026-09-27T21:01:00Z"),
            createHistoryActivity(id = "latest", occurredAt = "2026-09-28T10:00:00Z"),
        )

        // Act
        val groups = PolymarketActivityUMConverter(clock).convert(items)

        // Assert
        assertThat(groups.map { it.id }).containsExactly("2026-09-28", "2026-09-27").inOrder()
        assertThat(groups.first().title).isEqualTo(resourceReference(R.string.common_today))
        assertThat(groups.first().activities.map { it.id }).containsExactly("latest", "midnight").inOrder()
    }

    @Test
    fun `GIVEN a losing redemption WHEN converted THEN shows a loss without a positive payout or action`() {
        // Arrange
        val activity = createHistoryActivity(type = PolymarketActivity.Type.REDEEM)
            .copy(amount = BigDecimal.ZERO, redeemResult = PolymarketActivity.RedeemResult.LOST)
        val expected = PolymarketActivityRowUM(
            id = activity.id,
            type = PolymarketActivityRowUM.Type.LOST,
            title = resourceReference(R.string.prediction_positions_lost),
            subtitle = PolymarketActivitySubtitleUM.Plain(stringReference("")),
            amount = stringReference(MINUS),
        )

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a split redemption WHEN converted THEN labels the returned amount as redeemed`() {
        // Arrange
        val activity = createHistoryActivity(type = PolymarketActivity.Type.REDEEM)
            .copy(redeemResult = PolymarketActivity.RedeemResult.SPLIT)

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.type).isEqualTo(PolymarketActivityRowUM.Type.REDEEMED)
        assertThat(actual.title).isEqualTo(resourceReference(R.string.prediction_history_redeemed))
        assertThat(actual.amount).isEqualTo(stringReference("+ $10.00"))
    }

    @Test
    fun `GIVEN a withdrawal without funding metadata WHEN converted THEN uses its hash and a single negative sign`() {
        // Arrange
        val activity = createHistoryActivity(type = PolymarketActivity.Type.WITHDRAW)
            .copy(transactionHash = "0xtransaction", amount = BigDecimal("-10"))

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.amount).isEqualTo(stringReference("- $10.00"))
        assertThat(actual.subtitle).isEqualTo(PolymarketActivitySubtitleUM.Plain(stringReference("0xtransaction")))
    }
}

internal fun createHistoryActivity(
    id: String = "activity",
    type: PolymarketActivity.Type = PolymarketActivity.Type.BUY,
    occurredAt: String = "2026-09-28T10:00:00Z",
) = PolymarketActivity(id = id, type = type, occurredAt = Instant.parse(occurredAt), amount = BigDecimal.TEN)