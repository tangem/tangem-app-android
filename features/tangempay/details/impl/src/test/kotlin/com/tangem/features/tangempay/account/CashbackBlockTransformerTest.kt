package com.tangem.features.tangempay.account

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackBlockTransformerTest {

    private val defaultLocale = Locale.getDefault()

    private val onClick: () -> Unit = {}
    private val onGotIt: () -> Unit = {}
    private val dateFormatter = TangemPayCashbackDateFormatter()

    @BeforeEach
    fun setup() {
        // TangemPayCashbackDateFormatter -> DateTimeFormatters.dateMMMM -> android.text.format.DateFormat
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.formatDateRange(any(), any(), any()) } returns "July 1 – 5"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        unmockkObject(DateTimeFormatters)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN enabled summary WHEN transform THEN cashback block is widget with content`() {
        // Arrange
        val transformer = createTransformer(summary = enabledSummary())

        // Act
        val block = transformer.transform(contentState()).cashbackBlockState

        // Assert
        assertThat(block).isInstanceOf(CashbackBlockUM.Widget::class.java)
        val widget = block as CashbackBlockUM.Widget
        assertThat(widget.title).isInstanceOf(TextReference.Res::class.java)
        assertThat(widget.title).isNotEqualTo(TextReference.EMPTY)
        assertThat(widget.subtitle).isInstanceOf(TextReference.Res::class.java)
        assertThat(widget.subtitle).isNotEqualTo(TextReference.EMPTY)
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN summary and dismissed flag WHEN transform THEN resolves expected cashback block`(model: BlockCase) {
        // Arrange
        val transformer = createTransformer(summary = model.summary, isDismissed = model.isDismissed)

        // Act
        val block = transformer.transform(contentState()).cashbackBlockState

        // Assert
        assertThat(block).isEqualTo(model.expectedBlock)
    }

    private fun provideTestModels(): List<BlockCase> = listOf(
        BlockCase(
            description = "deactivated & not dismissed -> banner",
            summary = CashbackSummary.Deactivated,
            isDismissed = false,
            expectedBlock = CashbackBlockUM.DeactivatedBanner(onGotIt = onGotIt),
        ),
        BlockCase(
            description = "deactivated & dismissed -> hidden",
            summary = CashbackSummary.Deactivated,
            isDismissed = true,
            expectedBlock = null,
        ),
        BlockCase(
            description = "disabled -> hidden",
            summary = CashbackSummary.Disabled,
            isDismissed = false,
            expectedBlock = null,
        ),
        BlockCase(
            description = "unknown -> hidden",
            summary = CashbackSummary.Unknown,
            isDismissed = false,
            expectedBlock = null,
        ),
    )

    private fun createTransformer(
        summary: CashbackSummary,
        isDismissed: Boolean = false,
    ): CashbackBlockTransformer = CashbackBlockTransformer(
        summary = summary,
        isDeactivationDismissed = isDismissed,
        dateFormatter = dateFormatter,
        onClick = onClick,
        onGotIt = onGotIt,
    )

    private fun enabledSummary(
        confirmedAmount: BigDecimal = BigDecimal("32.15"),
        currency: String = "USD",
        year: Int = 2026,
        month: Int = 6,
        payoutStart: DateTime = DateTime.parse("2026-07-02"),
        payoutEnd: DateTime = DateTime.parse("2026-07-05"),
    ): CashbackSummary.Enabled = CashbackSummary.Enabled(
        displayMode = CashbackDisplayMode.FULL,
        cashback = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            pendingAmount = BigDecimal("13.65"),
            currency = currency,
            payoutCurrency = "USDC",
            payoutNetwork = "Polygon",
            period = TangemPayCashback.Period(
                year = year,
                month = month,
                payoutStart = payoutStart,
                payoutEnd = payoutEnd,
            ),
        ),
    )

    private fun contentState(): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = persistentListOf(),
        ),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
            actionButtons = persistentListOf(),
            cardsBlockState = null,
            fiatBalance = TextReference.EMPTY,
            isBalanceFlickering = false,
            isNegative = false,
            isInactive = false,
        ),
        isBalanceHidden = false,
        errorNotificationConfig = null,
        accountDeactivatedNotificationConfig = null,
    )

    internal class BlockCase(
        val summary: CashbackSummary,
        val isDismissed: Boolean,
        val expectedBlock: CashbackBlockUM?,
        private val description: String,
    ) {
        override fun toString(): String = description
    }
}