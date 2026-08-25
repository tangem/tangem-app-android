package com.tangem.features.tangempay.account

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
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

    @Test
    fun `GIVEN enabled summary without payout window WHEN transform THEN widget has no subtitle`() {
        // Arrange
        val summary = enabledSummary(payoutStart = null, payoutEnd = null)
        val transformer = createTransformer(summary = summary)

        // Act
        val block = transformer.transform(contentState()).cashbackBlockState

        // Assert
        val widget = block as CashbackBlockUM.Widget
        assertThat(widget.title).isInstanceOf(TextReference.Res::class.java)
        assertThat(widget.subtitle).isNull()
    }

    @Test
    fun `GIVEN enabled alt_block summary WHEN transform THEN cashback menu item inserted above terms`() {
        // Arrange
        val summary = enabledSummary(displayMode = CashbackDisplayMode.ALT_BLOCK)
        val transformer = createTransformer(summary = summary)
        val expectedTitle = resourceReference(
            id = R.string.tangempay_cashback_menu_item_title,
            formatArgs = wrappedList(dateFormatter.formatMonth(year = 2026, month = 6)),
        )

        // Act
        val items = transformer.transform(contentState()).topBarConfig.items

        // Assert
        val cashbackIndex = items.indexOfFirst { it.title == expectedTitle }
        val termsIndex = items.indexOfFirst { it.title == resourceReference(R.string.tangem_pay_terms_limits) }
        assertThat(cashbackIndex).isAtLeast(0)
        assertThat(termsIndex).isEqualTo(cashbackIndex + 1)
        assertThat(items[cashbackIndex].subtitle).isNull()
    }

    @Test
    fun `GIVEN enabled alt_block summary WHEN transform twice THEN cashback menu item not duplicated`() {
        // Arrange
        val transformer = createTransformer(summary = enabledSummary(displayMode = CashbackDisplayMode.ALT_BLOCK))

        // Act
        val state = transformer.transform(transformer.transform(contentState()))

        // Assert
        val cashbackItemCount = state.topBarConfig.items.count {
            (it.title as? TextReference.Res)?.id == R.string.tangempay_cashback_menu_item_title
        }
        assertThat(cashbackItemCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN enabled full summary WHEN transform THEN menu has no cashback item`() {
        // Arrange
        val transformer = createTransformer(summary = enabledSummary())

        // Act
        val items = transformer.transform(contentState()).topBarConfig.items

        // Assert
        val hasCashbackItem = items.any {
            (it.title as? TextReference.Res)?.id == R.string.tangempay_cashback_menu_item_title
        }
        assertThat(hasCashbackItem).isFalse()
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
            description = "enabled & alt_block -> hidden",
            summary = enabledSummary(displayMode = CashbackDisplayMode.ALT_BLOCK),
            isDismissed = false,
            expectedBlock = null,
        ),
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
        payoutStart: DateTime? = DateTime.parse("2026-07-02"),
        payoutEnd: DateTime? = DateTime.parse("2026-07-05"),
        displayMode: CashbackDisplayMode = CashbackDisplayMode.FULL,
    ): CashbackSummary.Enabled = CashbackSummary.Enabled(
        displayMode = displayMode,
        cashback = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            totalEarnedAmount = BigDecimal("132.15"),
            currency = currency,
            payoutCurrency = "USDC",
            period = TangemPayCashback.Period(
                year = year,
                month = month,
                payoutStart = payoutStart,
                payoutEnd = payoutEnd,
            ),
            previousPayout = null,
        ),
    )

    private fun contentState(): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = persistentListOf(
                menuItem(R.string.tangempay_current_plan_title),
                menuItem(R.string.tangem_pay_terms_limits),
                menuItem(R.string.tangempay_pay_support),
            ),
            subtitle = TextReference.EMPTY,
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

    private fun menuItem(titleRes: Int): TangemPayDropDownItemUM = TangemPayDropDownItemUM(
        title = resourceReference(titleRes),
        onClick = {},
        icon = TangemIconUM.Empty,
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