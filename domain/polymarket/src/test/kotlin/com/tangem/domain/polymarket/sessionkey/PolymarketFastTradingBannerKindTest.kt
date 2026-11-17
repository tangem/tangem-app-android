package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Duration
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFastTradingBannerKindTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a dismissal time WHEN isDismissed THEN hidden only within the kind period`(model: DismissalModel) {
        // Act
        val actual = model.kind.isDismissed(dismissedAt = model.dismissedAgo?.let(NOW::minus), now = NOW)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        DismissalModel(kind = PolymarketFastTradingBannerKind.ACTIVATE, dismissedAgo = null, expected = false),
        DismissalModel(kind = PolymarketFastTradingBannerKind.EXTEND, dismissedAgo = null, expected = false),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.ACTIVATE,
            dismissedAgo = Duration.ofDays(29),
            expected = true,
        ),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.ACTIVATE,
            dismissedAgo = Duration.ofDays(30),
            expected = false,
        ),
        DismissalModel(kind = PolymarketFastTradingBannerKind.REACTIVATE, dismissedAgo = null, expected = false),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.EXTEND,
            dismissedAgo = Duration.ofDays(29),
            expected = true,
        ),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.EXTEND,
            dismissedAgo = Duration.ofDays(30),
            expected = false,
        ),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.REACTIVATE,
            dismissedAgo = Duration.ofHours(23),
            expected = true,
        ),
        DismissalModel(
            kind = PolymarketFastTradingBannerKind.REACTIVATE,
            dismissedAgo = Duration.ofHours(24),
            expected = false,
        ),
    )

    data class DismissalModel(
        val kind: PolymarketFastTradingBannerKind,
        val dismissedAgo: Duration?,
        val expected: Boolean,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}