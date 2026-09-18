package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PolymarketPositionGlowTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Resolved {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN resolved holding WHEN converted THEN glow follows remaining pnl regardless of outcome and claim readiness`(
            model: GlowModel,
        ) {
            // Arrange
            val position = createPolymarketPosition(
                result = model.result,
                cashPnl = model.pnl.toBigDecimal(),
                realizedPnl = (-model.pnl).toBigDecimal(),
                outcome = model.outcome,
                resolutionStatus = PolymarketResolutionStatus.RESOLVED,
            ).copy(isRedeemable = model.redeemable)

            // Act
            val card = PolymarketPositionUMConverter().convert(position)

            // Assert
            assertThat(card.glowVariant).isEqualTo(model.glow)
        }

        private fun provideTestModels() = listOf(
            -12 to TangemGlowRing.Variant.Error,
            0 to null,
            12 to TangemGlowRing.Variant.Info,
        ).flatMap { (pnl, glow) ->
            listOf(PolymarketPositionResult.WON, PolymarketPositionResult.LOST, PolymarketPositionResult.SPLIT)
                .flatMap { result ->
                    listOf("Yes", "No").flatMap { outcome ->
                        listOf(true, false).map { redeemable -> GlowModel(result, pnl, outcome, redeemable, glow) }
                    }
                }
        }
    }

    @ParameterizedTest
    @EnumSource(PolymarketResolutionStatus::class)
    fun `GIVEN no settlement result WHEN converted THEN card has no glow and only a resolution badge`(
        resolution: PolymarketResolutionStatus,
    ) {
        // Arrange
        val position = createPolymarketPosition(resolutionStatus = resolution)
        val label = when (resolution) {
            PolymarketResolutionStatus.PROPOSED, PolymarketResolutionStatus.PENDING_RESOLUTION ->
                R.string.prediction_positions_pending
            PolymarketResolutionStatus.DISPUTED -> R.string.prediction_positions_disputed
            else -> null
        }

        // Act
        val card = PolymarketPositionUMConverter().convert(position)

        // Assert
        assertThat(card.glowVariant).isNull()
        assertThat(card.result).isEqualTo(
            label?.let {
                TangemBadgeUM(resourceReference(it), status = TangemBadge.Status.Warning, size = TangemBadge.Size.X6)
            },
        )
    }

    data class GlowModel(
        val result: PolymarketPositionResult,
        val pnl: Int,
        val outcome: String,
        val redeemable: Boolean,
        val glow: TangemGlowRing.Variant?,
    )
}