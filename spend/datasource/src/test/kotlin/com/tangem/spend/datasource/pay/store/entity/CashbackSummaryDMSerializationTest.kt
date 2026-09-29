package com.tangem.spend.datasource.pay.store.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CashbackSummaryDMSerializationTest {

    private val json = KotlinxDataStoreSerializer.jsonBuilder {}
    private val serializer = MapSerializer(String.serializer(), CashbackSummaryDM.serializer())

    @Test
    fun `GIVEN all cached summary types WHEN serialized and deserialized THEN value is preserved`() {
        // Arrange
        val original = mapOf(
            "wallet_full" to enabled(displayMode = CashbackSummaryDM.DisplayMode.FULL),
            "wallet_alt" to enabled(displayMode = CashbackSummaryDM.DisplayMode.ALT_BLOCK).copy(
                totalEarnedAmount = null,
                payoutCurrency = null,
                previousPayout = null,
                period = CashbackSummaryDM.Period(year = 2026, month = 1, payoutStart = null, payoutEnd = null),
            ),
            "wallet_deactivated" to CashbackSummaryDM.Deactivated,
        )

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, original))

        // Assert
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `GIVEN summary written with an unknown field WHEN deserialized THEN entry is kept`() {
        // Arrange
        val stored = """
            {"wallet":{"type":"enabled","display_mode":"alt_block","confirmed_amount":"2.70","currency":"USD",
            "period":{"year":2026,"month":8},"field_from_newer_app":true}}
        """.trimIndent()

        // Act
        val restored = json.decodeFromString(serializer, stored)

        // Assert
        assertThat(restored).isEqualTo(
            mapOf(
                "wallet" to CashbackSummaryDM.Enabled(
                    displayMode = CashbackSummaryDM.DisplayMode.ALT_BLOCK,
                    confirmedAmount = BigDecimal("2.70"),
                    currency = "USD",
                    period = CashbackSummaryDM.Period(year = 2026, month = 8),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN cached summary WHEN serialized THEN uses stable type names`() {
        // Act
        val encoded = json.encodeToString(serializer, mapOf("wallet" to CashbackSummaryDM.Deactivated))

        // Assert
        assertThat(encoded).isEqualTo("""{"wallet":{"type":"deactivated"}}""")
    }

    private fun enabled(displayMode: CashbackSummaryDM.DisplayMode) = CashbackSummaryDM.Enabled(
        displayMode = displayMode,
        confirmedAmount = BigDecimal("32.15"),
        totalEarnedAmount = BigDecimal("132.15"),
        currency = "USD",
        payoutCurrency = "USDC",
        period = CashbackSummaryDM.Period(
            year = 2026,
            month = 6,
            payoutStart = DateTime(PAYOUT_START_MILLIS),
            payoutEnd = DateTime(PAYOUT_END_MILLIS),
        ),
        previousPayout = CashbackSummaryDM.PreviousPayout(
            endDate = DateTime(PAYOUT_END_MILLIS),
            amount = BigDecimal("18.00"),
        ),
    )

    private companion object {
        const val PAYOUT_START_MILLIS = 1_782_950_400_000L
        const val PAYOUT_END_MILLIS = 1_783_209_600_000L
    }
}