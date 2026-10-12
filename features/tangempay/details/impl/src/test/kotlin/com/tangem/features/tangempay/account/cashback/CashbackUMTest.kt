package com.tangem.features.tangempay.account.cashback

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.account.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.account.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.account.TangemPayDetailsUM
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

internal class CashbackUMTest {

    private val currentPlanItem = menuItem(R.string.tangempay_current_plan_title)
    private val supportItem = menuItem(R.string.tangempay_pay_support)
    private val cashbackItem = menuItem(R.string.tangempay_cashback_menu_item_title)

    @Test
    fun `GIVEN menu item and current plan entry WHEN withCashback THEN item inserted right below current plan`() {
        // Arrange
        val state = detailsState(currentPlanItem, supportItem)

        // Act
        val actual = state.withCashback(CashbackUM(menuItem = cashbackItem))

        // Assert
        assertThat(actual.topBarConfig.items).containsExactly(currentPlanItem, cashbackItem, supportItem).inOrder()
    }

    @Test
    fun `GIVEN menu item without current plan entry WHEN withCashback THEN item inserted first`() {
        // Arrange
        val state = detailsState(supportItem)

        // Act
        val actual = state.withCashback(CashbackUM(menuItem = cashbackItem))

        // Assert
        assertThat(actual.topBarConfig.items).containsExactly(cashbackItem, supportItem).inOrder()
    }

    @Test
    fun `GIVEN no menu item WHEN withCashback THEN state unchanged`() {
        // Arrange
        val state = detailsState(currentPlanItem, supportItem)

        // Act
        val actual = state.withCashback(CashbackUM())

        // Assert
        assertThat(actual).isEqualTo(state)
    }

    @Test
    fun `GIVEN block WHEN withCashback THEN cashback block set and menu untouched`() {
        // Arrange
        val state = detailsState(currentPlanItem, supportItem)
        val block = CashbackBlockUM.DeactivatedBanner(onGotIt = {})

        // Act
        val actual = state.withCashback(CashbackUM(block = block))

        // Assert
        assertThat(actual).isEqualTo(state.copy(cashbackBlockState = block))
    }

    private fun detailsState(vararg menuItems: TangemPayDropDownItemUM): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = menuItems.toList().toImmutableList(),
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
        messageBanner = null,
    )

    private fun menuItem(titleRes: Int): TangemPayDropDownItemUM = TangemPayDropDownItemUM(
        title = resourceReference(titleRes),
        onClick = {},
        icon = TangemIconUM.Empty,
    )
}