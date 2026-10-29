package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivity.RedeemResult
import com.tangem.domain.polymarket.model.PolymarketActivity.Type
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketActivityClaimMergerTest {

    @Test
    fun `GIVEN claim rows with different casing WHEN merging THEN sums amounts and preserves the first position`() {
        // Arrange
        val before = createHistoryActivity(id = "before")
        val between = createHistoryActivity(id = "between")
        val lost = claim(id = "lost", result = RedeemResult.LOST, amount = "0.000000")
        val won = claim(id = "won", amount = "400.000001").copy(
            transactionHash = "0xAbC",
            occurredAt = Instant.parse("2026-09-27T12:00:00Z"),
            market = requireNotNull(lost.market).copy(conditionId = "0xDeF", outcome = "Yes", outcomeIndex = 0),
        )
        val anotherWin = won.copy(id = "another-win", amount = BigDecimal("0.123456"))

        // Act
        val actual = PolymarketActivityClaimMerger.merge(listOf(before, lost, between, won, anotherWin))

        // Assert
        assertThat(actual).containsExactly(
            before,
            lost.copy(amount = BigDecimal("400.123457"), market = won.market, redeemResult = RedeemResult.WON),
            between,
        ).inOrder()
    }

    @Test
    fun `GIVEN a claim crossing pages WHEN the winner is appended THEN merges without moving the first row`() {
        // Arrange
        val lost = claim(id = "lost", result = RedeemResult.LOST, amount = "0.000000")
        val won = claim(id = "won", amount = "400.000000")
        val firstPage = listOf(createHistoryActivity(id = "before"), lost)

        // Act
        val beforeLoadingMore = PolymarketActivityClaimMerger.merge(firstPage)
        val afterLoadingMore = PolymarketActivityClaimMerger.merge(firstPage + won)

        // Assert
        assertThat(beforeLoadingMore).containsExactlyElementsIn(firstPage).inOrder()
        assertThat(afterLoadingMore).containsExactly(
            firstPage.first(), lost.copy(amount = won.amount, market = won.market, redeemResult = RedeemResult.WON),
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN incomplete or different claim identity WHEN merging THEN keeps rows separate`(
        other: PolymarketActivity,
    ) {
        // Arrange
        val items = listOf(claim(id = "winner"), other)

        // Act
        val actual = PolymarketActivityClaimMerger.merge(items)

        // Assert
        assertThat(actual).containsExactlyElementsIn(items).inOrder()
    }

    @ParameterizedTest
    @EnumSource(value = RedeemResult::class, names = ["LOST", "SPLIT"])
    fun `GIVEN a claim without a winning row WHEN merging THEN preserves its original result`(result: RedeemResult) {
        // Arrange
        val items = listOf(claim(id = "first", result = result), claim(id = "second", result = result))

        // Act
        val actual = PolymarketActivityClaimMerger.merge(items)

        // Assert
        assertThat(actual).containsExactlyElementsIn(items).inOrder()
    }

    @Test
    fun `GIVEN a claim with lost and won rows WHEN opening its row THEN details receive the merged claim`() {
        // Arrange
        val won = claim(id = "won")
        val lost = won.copy(id = "lost", amount = BigDecimal.ZERO, redeemResult = RedeemResult.LOST)
        var clicked: PolymarketActivity? = null

        // Act
        val rows = PolymarketActivityUMConverter().convert(listOf(lost, won)) { clicked = it }
            .single().activities
        requireNotNull(rows.single().onClick).invoke()

        // Assert
        assertThat(rows.single().id).isEqualTo("lost")
        assertThat(rows.single().type).isEqualTo(PolymarketActivityRowUM.Type.REDEEMED)
        assertThat(clicked).isEqualTo(won.copy(id = "lost"))
    }

    private fun provideTestModels(): List<PolymarketActivity> {
        val other = claim(id = "other", result = RedeemResult.LOST)
        return listOf(
            other.copy(transactionHash = null),
            other.copy(transactionHash = ""),
            other.copy(transactionHash = " "),
            other.copy(transactionHash = "0x999"),
            other.copy(market = null),
            other.copy(market = requireNotNull(other.market).copy(conditionId = "")),
            other.copy(market = requireNotNull(other.market).copy(conditionId = " ")),
            other.copy(market = requireNotNull(other.market).copy(conditionId = "0x999")),
            other.copy(type = Type.BUY),
        )
    }

    private fun claim(
        id: String = "claim",
        result: RedeemResult = RedeemResult.WON,
        amount: String = "10.00",
    ): PolymarketActivity = historyTrade(Type.REDEEM).let {
        it.copy(
            id = id,
            amount = BigDecimal(amount),
            transactionHash = "0xabc",
            redeemResult = result,
            market = requireNotNull(it.market).copy(conditionId = "0xdef"),
        )
    }
}