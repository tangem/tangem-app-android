package com.tangem.features.tangempay.account.cashback

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackUMFactoryTest {

    private val defaultLocale = Locale.getDefault()

    private val onWidgetClick: () -> Unit = {}
    private val onMenuItemClick: () -> Unit = {}
    private val onGotIt: () -> Unit = {}
    private val onReload: () -> Unit = {}

    private val factory = CashbackUMFactory(
        dateFormatter = TangemPayCashbackDateFormatter(),
        onWidgetClick = onWidgetClick,
        onMenuItemClick = onMenuItemClick,
        onGotIt = onGotIt,
        onReload = onReload,
    )

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN hidden content WHEN create THEN neither block nor menu item`() {
        // Act
        val actual = factory.create(CashbackContent.Hidden)

        // Assert
        assertThat(actual).isEqualTo(CashbackUM())
    }

    @Test
    fun `GIVEN widget content WHEN create THEN widget block with amount and month and no menu item`() {
        // Arrange
        val content = CashbackContent.Widget(cashback())

        // Act
        val actual = factory.create(content)

        // Assert
        val expected = CashbackUM(
            block = CashbackBlockUM.Widget(
                title = resourceReference(
                    id = R.string.tangempay_cashback_widget_title,
                    formatArgs = wrappedList("$32.15", "June"),
                ),
                isNegative = false,
                onClick = onWidgetClick,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN confirmed amount WHEN create widget THEN title and negative state follow the amount`(
        model: AmountModel,
    ) {
        // Arrange
        val content = CashbackContent.Widget(cashback(confirmedAmount = model.amount))

        // Act
        val actual = factory.create(content)

        // Assert
        val expected = CashbackUM(
            block = CashbackBlockUM.Widget(
                title = resourceReference(
                    id = R.string.tangempay_cashback_widget_title,
                    formatArgs = wrappedList(model.expectedAmount, "June"),
                ),
                isNegative = model.expectedNegative,
                onClick = onWidgetClick,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN menu entry content WHEN create THEN menu item with month and no block`() {
        // Arrange
        val content = CashbackContent.MenuEntry(cashback())

        // Act
        val actual = factory.create(content)

        // Assert
        val actualTint = (actual.menuItem?.icon as TangemIconUM.Icon).tint
        val expected = CashbackUM(
            menuItem = TangemPayDropDownItemUM(
                title = resourceReference(
                    id = R.string.tangempay_cashback_menu_item_title,
                    formatArgs = wrappedList("June"),
                ),
                onClick = onMenuItemClick,
                icon = TangemIconUM.Icon(imageVector = Icons.ic_percent_backward_20, tintReference = actualTint),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN deactivation banner content WHEN create THEN deactivated banner block`() {
        // Act
        val actual = factory.create(CashbackContent.DeactivationBanner)

        // Assert
        assertThat(actual).isEqualTo(CashbackUM(block = CashbackBlockUM.DeactivatedBanner(onGotIt = onGotIt)))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN unavailable content WHEN create THEN error block reloading while retrying`(isRetrying: Boolean) {
        // Act
        val actual = factory.create(CashbackContent.Unavailable(isRetrying = isRetrying))

        // Assert
        val expected = CashbackUM(block = CashbackBlockUM.Error(onReload = onReload, isReloading = isRetrying))
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels(): List<AmountModel> = listOf(
        AmountModel(
            description = "negative -> negative flag",
            amount = BigDecimal("-2.15"),
            expectedAmount = "-$2.15",
            expectedNegative = true,
        ),
        AmountModel(
            description = "zero -> two decimals",
            amount = BigDecimal.ZERO,
            expectedAmount = "$0.00",
            expectedNegative = false,
        ),
        AmountModel(
            description = "zero with scale -> two decimals",
            amount = BigDecimal("0.00"),
            expectedAmount = "$0.00",
            expectedNegative = false,
        ),
        AmountModel(
            description = "positive -> two decimals",
            amount = BigDecimal("32.15"),
            expectedAmount = "$32.15",
            expectedNegative = false,
        ),
    )

    private fun cashback(confirmedAmount: BigDecimal = BigDecimal("32.15")): TangemPayCashback = TangemPayCashback(
        confirmedAmount = confirmedAmount,
        totalEarnedAmount = BigDecimal("132.15"),
        currency = "USD",
        payoutCurrency = "USDC",
        period = TangemPayCashback.Period(year = 2026, month = 6, payoutStart = null, payoutEnd = null),
        previousPayout = null,
    )

    internal data class AmountModel(
        val description: String,
        val amount: BigDecimal,
        val expectedAmount: String,
        val expectedNegative: Boolean,
    ) {
        override fun toString(): String = description
    }
}