package com.tangem.features.tangempay.account

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

internal class CashbackErrorBlockTransformerTest {

    private val onReload: () -> Unit = {}
    private val transformer = CashbackErrorBlockTransformer(onReload = onReload)

    @Test
    fun `GIVEN state with cashback menu item WHEN transform THEN error block set and menu item removed`() {
        // Arrange
        val state = contentState(withCashbackMenuItem = true)

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result.cashbackBlockState).isEqualTo(CashbackBlockUM.Error(onReload = onReload))
        val menuTitleIds = result.topBarConfig.items.mapNotNull { (it.title as? TextReference.Res)?.id }
        assertThat(menuTitleIds).containsExactly(
            R.string.tangempay_current_plan_title,
            R.string.tangem_pay_terms_limits,
            R.string.tangempay_pay_support,
        ).inOrder()
    }

    @Test
    fun `GIVEN state without cashback menu item WHEN transform THEN error block set and menu untouched`() {
        // Arrange
        val state = contentState(withCashbackMenuItem = false)

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result.cashbackBlockState).isEqualTo(CashbackBlockUM.Error(onReload = onReload))
        assertThat(result.topBarConfig.items).isEqualTo(state.topBarConfig.items)
    }

    private fun contentState(withCashbackMenuItem: Boolean): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = buildList {
                add(menuItem(R.string.tangempay_current_plan_title))
                if (withCashbackMenuItem) add(menuItem(R.string.tangempay_cashback_menu_item_title))
                add(menuItem(R.string.tangem_pay_terms_limits))
                add(menuItem(R.string.tangempay_pay_support))
            }.toImmutableList(),
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
}