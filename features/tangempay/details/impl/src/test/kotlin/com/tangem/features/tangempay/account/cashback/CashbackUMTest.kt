package com.tangem.features.tangempay.account.cashback

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.account.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.account.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.account.TangemPayDetailsUM
import com.tangem.features.tangempay.common.PayMenuItems
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.tariffPlanState
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

    @Test
    fun `GIVEN ds menu with change plan WHEN withCashback THEN cashback entry inserted right below change plan`() {
        // Arrange
        val state = detailsState(currentPlanItem, supportItem).withDsMenu(DS_CHANGE_PLAN_ENTRY, DS_SUPPORT_ENTRY)

        // Act
        val actual = state.withCashback(CashbackUM(menuItem = cashbackItem))

        // Assert
        assertThat(actual.topBarConfig.menu?.entries)
            .containsExactly(DS_CHANGE_PLAN_ENTRY, dsCashbackEntry(), DS_SUPPORT_ENTRY)
            .inOrder()
    }

    @Test
    fun `GIVEN ds menu without current plan WHEN withCashback THEN cashback entry inserted first`() {
        // Arrange
        val state = detailsState(supportItem).withDsMenu(DS_SUPPORT_ENTRY)

        // Act
        val actual = state.withCashback(CashbackUM(menuItem = cashbackItem))

        // Assert
        assertThat(actual.topBarConfig.menu?.entries).containsExactly(dsCashbackEntry(), DS_SUPPORT_ENTRY).inOrder()
    }

    @Test
    fun `GIVEN no ds menu WHEN withCashback THEN menu stays absent`() {
        // Arrange
        val state = detailsState(currentPlanItem, supportItem)

        // Act
        val actual = state.withCashback(CashbackUM(menuItem = cashbackItem))

        // Assert
        assertThat(actual.topBarConfig.menu).isNull()
    }

    private fun dsCashbackEntry(): TangemMenu.UiState.Item = TangemMenu.UiState.Item(
        label = cashbackItem.title,
        onClick = cashbackItem.onClick,
        iconStart = Icons.ic_percent_backward_20,
    )

    private fun TangemPayDetailsUM.withDsMenu(vararg entries: TangemMenu.UiState.Entry): TangemPayDetailsUM =
        copy(topBarConfig = topBarConfig.copy(menu = TangemMenu.UiState(entries = persistentListOf(*entries))))

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

    private companion object {
        val DS_CHANGE_PLAN_ENTRY = PayMenuItems.changePlan(tariffPlan = tariffPlanState(), onClick = {})
        val DS_SUPPORT_ENTRY = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_pay_support),
            onClick = {},
        )
    }
}