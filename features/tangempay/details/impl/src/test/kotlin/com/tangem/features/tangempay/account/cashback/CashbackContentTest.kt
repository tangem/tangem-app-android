package com.tangem.features.tangempay.account.cashback

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

internal class CashbackContentTest {

    private val cashback = TangemPayCashback(
        confirmedAmount = BigDecimal("32.15"),
        totalEarnedAmount = BigDecimal("132.15"),
        currency = "USD",
        payoutCurrency = "USDC",
        period = TangemPayCashback.Period(year = 2026, month = 6, payoutStart = null, payoutEnd = null),
        previousPayout = null,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveCashbackContent {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN summary refresh state and dismissed flag WHEN resolve THEN expected content`(model: ResolveModel) {
            // Act
            val actual = resolveCashbackContent(
                summary = model.summary,
                refreshState = model.refreshState,
                isDeactivationDismissed = model.isDeactivationDismissed,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ResolveModel> = listOf(
            ResolveModel(
                description = "nothing cached, refresh failed -> unavailable",
                summary = null,
                refreshState = CashbackRefreshState(isFailed = true),
                expected = CashbackContent.Unavailable(isRetrying = false),
            ),
            ResolveModel(
                description = "nothing cached, refresh failed, retry in flight -> unavailable and retrying",
                summary = null,
                refreshState = CashbackRefreshState(isFailed = true, isInProgress = true),
                expected = CashbackContent.Unavailable(isRetrying = true),
            ),
            ResolveModel(
                description = "nothing cached, no failed refresh -> hidden",
                summary = null,
                refreshState = CashbackRefreshState(),
                expected = CashbackContent.Hidden,
            ),
            ResolveModel(
                description = "nothing cached, first refresh in flight -> hidden",
                summary = null,
                refreshState = CashbackRefreshState(isInProgress = true),
                expected = CashbackContent.Hidden,
            ),
            ResolveModel(
                description = "enabled full -> widget",
                summary = enabledSummary(CashbackDisplayMode.FULL),
                expected = CashbackContent.Widget(cashback),
            ),
            ResolveModel(
                description = "enabled full, refresh failed -> cached widget",
                summary = enabledSummary(CashbackDisplayMode.FULL),
                refreshState = CashbackRefreshState(isFailed = true),
                expected = CashbackContent.Widget(cashback),
            ),
            ResolveModel(
                description = "enabled alt_block -> menu entry",
                summary = enabledSummary(CashbackDisplayMode.ALT_BLOCK),
                expected = CashbackContent.MenuEntry(cashback),
            ),
            ResolveModel(
                description = "enabled alt_block, refresh failed -> cached menu entry",
                summary = enabledSummary(CashbackDisplayMode.ALT_BLOCK),
                refreshState = CashbackRefreshState(isFailed = true),
                expected = CashbackContent.MenuEntry(cashback),
            ),
            ResolveModel(
                description = "deactivated, not dismissed -> deactivation banner",
                summary = CashbackSummary.Deactivated,
                expected = CashbackContent.DeactivationBanner,
            ),
            ResolveModel(
                description = "deactivated, dismissed -> hidden",
                summary = CashbackSummary.Deactivated,
                isDeactivationDismissed = true,
                expected = CashbackContent.Hidden,
            ),
            ResolveModel(
                description = "disabled -> hidden",
                summary = CashbackSummary.Disabled,
                expected = CashbackContent.Hidden,
            ),
            ResolveModel(
                description = "unknown -> hidden",
                summary = CashbackSummary.Unknown,
                expected = CashbackContent.Hidden,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Impression {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN content WHEN impression THEN expected impression`(model: ImpressionModel) {
            // Act
            val actual = model.content.impression()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ImpressionModel> = listOf(
            ImpressionModel(content = CashbackContent.Hidden, expected = null),
            ImpressionModel(content = CashbackContent.Widget(cashback), expected = CashbackImpression.Widget),
            ImpressionModel(
                content = CashbackContent.MenuEntry(cashback),
                expected = CashbackImpression.SettingsButton,
            ),
            ImpressionModel(
                content = CashbackContent.DeactivationBanner,
                expected = CashbackImpression.DeactivationBanner,
            ),
            ImpressionModel(
                content = CashbackContent.Unavailable(isRetrying = false),
                expected = CashbackImpression.Error,
            ),
            ImpressionModel(
                content = CashbackContent.Unavailable(isRetrying = true),
                expected = CashbackImpression.Error,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToAnalyticsEvent {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN impression WHEN toAnalyticsEvent THEN matching shown event`(model: AnalyticsEventModel) {
            // Act
            val actual = model.impression.toAnalyticsEvent()

            // Assert
            assertThat(actual).isInstanceOf(model.expected.java)
        }

        private fun provideTestModels(): List<AnalyticsEventModel> = listOf(
            AnalyticsEventModel(
                impression = CashbackImpression.Widget,
                expected = TangemPayAnalyticsEvents.Cashback.BannerShowed::class,
            ),
            AnalyticsEventModel(
                impression = CashbackImpression.SettingsButton,
                expected = TangemPayAnalyticsEvents.Cashback.ButtonInSettingsShowed::class,
            ),
            AnalyticsEventModel(
                impression = CashbackImpression.DeactivationBanner,
                expected = TangemPayAnalyticsEvents.Cashback.DeactivationBannerShowed::class,
            ),
            AnalyticsEventModel(
                impression = CashbackImpression.Error,
                expected = TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed::class,
            ),
        )
    }

    private fun enabledSummary(displayMode: CashbackDisplayMode): CashbackSummary.Enabled =
        CashbackSummary.Enabled(displayMode = displayMode, cashback = cashback)

    internal data class ResolveModel(
        val description: String,
        val summary: CashbackSummary?,
        val refreshState: CashbackRefreshState = CashbackRefreshState(),
        val isDeactivationDismissed: Boolean = false,
        val expected: CashbackContent,
    ) {
        override fun toString(): String = description
    }

    internal data class ImpressionModel(
        val content: CashbackContent,
        val expected: CashbackImpression?,
    )

    internal data class AnalyticsEventModel(
        val impression: CashbackImpression,
        val expected: KClass<out TangemPayAnalyticsEvents>,
    )
}