package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivity.RedeemResult
import com.tangem.domain.polymarket.model.PolymarketActivity.Type
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class PolymarketActivityDetailsConverterTest {

    private val previousLocale = Locale.getDefault()
    private val converter = PolymarketActivityDetailsConverter(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.ofHours(3)),
    )

    @BeforeEach
    fun setLocale() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `GIVEN a buy WHEN opening details THEN shows executed values and combined fees`() {
        // Arrange
        val activity = historyTrade().copy(amount = BigDecimal("5.072477"))

        // Act
        val result = converter.convert(activity, null)

        // Assert
        assertThat(result.amount).isEqualTo(stringReference("-$5.07"))
        assertThat(result.rows.map { it.value }).containsExactly(
            stringReference("6.17"), stringReference("$0.81"), stringReference("$0.07"), stringReference("$5.00"),
        ).inOrder()
        assertThat(result.outcome).isEqualTo(stringReference("Aurora Gaming"))
        assertThat(result.date).isEqualTo(stringReference("2026-09-28 13:00"))
    }

    @Test
    fun `GIVEN a sell WHEN opening details THEN shows unsigned proceeds and precise price`() {
        // Arrange
        val activity = historyTrade(Type.SELL).copy(
            amount = BigDecimal("-4.934950"),
            order = historyTrade().order!!.copy(averagePrice = BigDecimal("0.001500")),
        )

        // Act
        val result = converter.convert(activity, null)

        // Assert
        assertThat(result.amount).isEqualTo(stringReference("$4.93"))
        assertThat(result.rows[1].value).isEqualTo(stringReference("$0.0015"))
        assertThat(result.rows.count { it.title == resourceReference(R.string.prediction_positions_quantity) })
            .isEqualTo(1)
    }

    @Test
    fun `GIVEN a redemption WHEN opening details THEN distinguishes the result and hides order metrics`() {
        // Arrange
        val headlines = mapOf(
            RedeemResult.WON to R.string.prediction_history_prediction_won,
            RedeemResult.LOST to R.string.prediction_history_prediction_lost,
            RedeemResult.SPLIT to R.string.prediction_history_prediction_split,
        )
        headlines.forEach { (status, headline) ->
            val activity = historyTrade(Type.REDEEM).copy(redeemResult = status)

            // Act
            val result = converter.convert(activity, null)

            // Assert
            assertThat(result.headline).isEqualTo(resourceReference(headline))
            assertThat(result.rows).isEmpty()
            assertThat(result.amount).isEqualTo(if (status == RedeemResult.LOST) null else stringReference("$10.00"))
        }
    }

    @Test
    fun `GIVEN missing metadata WHEN opening details THEN does not invent an outcome or fees`() {
        // Arrange
        val activity = createHistoryActivity()

        // Act
        val result = converter.convert(activity, null)

        // Assert
        assertThat(result.outcome).isNull()
        assertThat(result.rows).isEmpty()
        assertThat(result.onGoToPrediction).isNull()
    }
    @Test
    fun `GIVEN a redeemed loss without an order WHEN opening details THEN does not invent stake or entry price`() {
        // Arrange
        val activity = historyTrade(Type.REDEEM).copy(
            amount = BigDecimal.ZERO,
            redeemResult = RedeemResult.LOST,
            order = null,
        )

        // Act
        val result = converter.convert(activity, null)

        // Assert
        assertThat(result.amount).isNull()
        assertThat(result.outcomePrice).isNull()
        assertThat(result.headline).isEqualTo(resourceReference(R.string.prediction_history_prediction_lost))
    }

}