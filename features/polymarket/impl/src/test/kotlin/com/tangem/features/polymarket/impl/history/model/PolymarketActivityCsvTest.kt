package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketActivity.Type
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.Instant

internal class PolymarketActivityCsvTest {

    @Test
    fun `GIVEN an executed trade WHEN exporting THEN preserves raw precision and column order`() {
        // Arrange
        val record = historyTrade().copy(occurredAt = Instant.parse("2026-10-16T12:34:56.123Z"))

        // Act
        val csv = PolymarketActivityCsv.make(listOf(record), listOf("Header"))

        // Assert
        assertThat(csv).isEqualTo(
            "\"Header\"\r\n" +
                "\"2026-10-16T12:34:56Z\",\"BUY\",\"\",\"-10\",\"Counter-Strike: 1WIN vs Aurora Gaming\"," +
                "\"Counter-Strike\",\"Aurora Gaming\",\"6.172836\",\"0.81\",\"0.04749\",\"0.02499\",\"4.999997\",\"\"",
        )
    }

    @ParameterizedTest
    @CsvSource("BUY,5,-5", "WITHDRAW,5,-5", "BUY,-5,-5", "SELL,5,5", "DEPOSIT,5,5", "REDEEM,5,5", "BUY,0,0")
    fun `GIVEN an operation WHEN exporting THEN signs money like iOS`(type: Type, amount: String, expected: String) {
        // Arrange
        val record = createHistoryActivity(type = type).copy(amount = BigDecimal(amount))

        // Act
        val line = PolymarketActivityCsv.make(listOf(record), emptyList()).substringAfter("\r\n")

        // Assert
        assertThat(line.split(',')[3]).isEqualTo("\"$expected\"")
    }

    @ParameterizedTest
    @ValueSource(strings = ["=", "+", "-", "@", "\t", "\r", "\n"])
    fun `GIVEN spreadsheet formula text WHEN exporting THEN escapes formulas quotes commas and newlines`(prefix: String) {
        // Arrange
        val record = historyTrade()
        val title = "${prefix}HYPERLINK(\"x,y\")\nline"
        val market = requireNotNull(record.market).copy(title = title, outcome = title)

        // Act
        val csv = PolymarketActivityCsv.make(listOf(record.copy(market = market)), emptyList())

        // Assert
        assertThat(csv).contains("\"'${prefix}HYPERLINK(\"\"x,y\"\")\nline\"")
    }

    @Test
    fun `GIVEN absent optional data WHEN exporting THEN keeps empty columns`() {
        // Arrange
        val record = createHistoryActivity(type = Type.DEPOSIT).copy(occurredAt = Instant.EPOCH)

        // Act
        val csv = PolymarketActivityCsv.make(listOf(record), emptyList())

        // Assert
        assertThat(csv).isEqualTo(
            "\r\n\"1970-01-01T00:00:00Z\",\"DEPOSIT\",\"\",\"10\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
        )
    }
}