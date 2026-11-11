package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.MINUS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun `GIVEN a split redemption WHEN converted THEN shows a split with its market title and returned amount`() {
        // Arrange
        val activity = historyTrade(PolymarketActivity.Type.REDEEM)
            .copy(redeemResult = PolymarketActivity.RedeemResult.SPLIT, amount = BigDecimal("12.000000"))

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.type).isEqualTo(PolymarketActivityRowUM.Type.SPLIT)
        assertThat(actual.title).isEqualTo(resourceReference(R.string.prediction_positions_split))
        assertThat(actual.subtitle).isEqualTo(PolymarketActivitySubtitleUM.Plain(stringReference(activity.marketTitle)))
        assertThat(actual.amount).isEqualTo(stringReference("+ $12.00"))
    }

    @Test
    fun `GIVEN a withdrawal without a market WHEN converted THEN keeps its raw hash and a single negative sign`() {
        // Arrange
        val activity = createHistoryActivity(type = PolymarketActivity.Type.WITHDRAW)
            .copy(transactionHash = "0xtransaction", amount = BigDecimal("-10"))
        val expected = PolymarketActivityRowUM(
            id = activity.id,
            type = PolymarketActivityRowUM.Type.WITHDRAWN,
            title = resourceReference(R.string.transaction_history_withdrawn),
            subtitle = PolymarketActivitySubtitleUM.TransactionHash("0xtransaction"),
            amount = stringReference("- $10.00"),
        )

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a deposit with a blank hash WHEN converted THEN shows an empty subtitle`() {
        // Arrange
        val activity = createHistoryActivity(type = PolymarketActivity.Type.DEPOSIT).copy(transactionHash = " ")

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.subtitle).isEqualTo(PolymarketActivitySubtitleUM.Plain(stringReference("")))
    }

    @Test
    fun `GIVEN a trade with a market and a hash WHEN converted THEN shows the market title instead of the hash`() {
        // Arrange
        val activity = historyTrade(PolymarketActivity.Type.BUY).copy(transactionHash = "0xtransaction")

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.subtitle).isEqualTo(PolymarketActivitySubtitleUM.Plain(stringReference(activity.marketTitle)))
    }

    @Test
    fun `GIVEN a won redemption and buy WHEN converting THEN describes the win and makes both clickable`() {
        // Arrange
        val won = historyTrade(PolymarketActivity.Type.REDEEM).copy(redeemResult = PolymarketActivity.RedeemResult.WON)
        val deposit = createHistoryActivity(id = "buy", type = PolymarketActivity.Type.BUY)

        // Act
        val rows = PolymarketActivityUMConverter(clock).convert(listOf(won, deposit)) {}.single().activities

        // Assert
        assertThat(rows.first().subtitle).isEqualTo(
            PolymarketActivitySubtitleUM.Plain(
                resourceReference(R.string.prediction_history_won_description, wrappedList(won.marketTitle)),
            ),
        )
        assertThat(rows.first().onClick).isNotNull()
        assertThat(rows.last().onClick).isNotNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an activity amount WHEN converted THEN the row shows whole cents or a dash when they round to zero`(
        case: RowAmountCase,
    ) {
        // Arrange
        val activity = createHistoryActivity(type = case.type)
            .copy(amount = BigDecimal(case.amount), redeemResult = case.redeemResult)

        // Act
        val actual = PolymarketActivityUMConverter(clock).convert(listOf(activity)).single().activities.single()

        // Assert
        assertThat(actual.amount).isEqualTo(stringReference(case.expected))
    }

    private fun provideTestModels() = listOf(
        RowAmountCase(PolymarketActivity.Type.WITHDRAW, amount = "0.004", expected = MINUS),
        RowAmountCase(PolymarketActivity.Type.DEPOSIT, amount = "0.006", expected = "+ $0.01"),
        RowAmountCase(PolymarketActivity.Type.WITHDRAW, amount = "0.006", expected = "- $0.01"),
        RowAmountCase(PolymarketActivity.Type.BUY, amount = "0.005", expected = "- $0.01"),
        RowAmountCase(PolymarketActivity.Type.SELL, amount = "0.004999999", expected = MINUS),
        RowAmountCase(PolymarketActivity.Type.DEPOSIT, amount = "0", expected = MINUS),
        RowAmountCase(PolymarketActivity.Type.WITHDRAW, amount = "-0.004", expected = MINUS),
        RowAmountCase(PolymarketActivity.Type.WITHDRAW, amount = "-10", expected = "- $10.00"),
        RowAmountCase(
            PolymarketActivity.Type.REDEEM,
            PolymarketActivity.RedeemResult.WON,
            amount = "9.995",
            expected = "+ $10.00",
        ),
        RowAmountCase(
            PolymarketActivity.Type.REDEEM,
            PolymarketActivity.RedeemResult.SPLIT,
            amount = "0.004",
            expected = MINUS,
        ),
        RowAmountCase(
            PolymarketActivity.Type.REDEEM,
            PolymarketActivity.RedeemResult.LOST,
            amount = "5",
            expected = MINUS,
        ),
    )

    data class RowAmountCase(
        val type: PolymarketActivity.Type,
        val redeemResult: PolymarketActivity.RedeemResult? = null,
        val amount: String,
        val expected: String,
    )
}

internal fun createHistoryActivity(
    id: String = "activity",
    type: PolymarketActivity.Type = PolymarketActivity.Type.BUY,
    occurredAt: String = "2026-09-28T10:00:00Z",
) = PolymarketActivity(id = id, type = type, occurredAt = Instant.parse(occurredAt), amount = BigDecimal.TEN)