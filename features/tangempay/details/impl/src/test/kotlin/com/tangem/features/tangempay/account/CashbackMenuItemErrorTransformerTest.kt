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

internal class CashbackMenuItemErrorTransformerTest {

    private val onReload: () -> Unit = {}

    @Test
    fun `GIVEN regular cashback entry WHEN transform THEN error entry replaces it below current plan`() {
        // Arrange
        val transformer = CashbackMenuItemErrorTransformer(onReload = onReload, isReloading = false)

        // Act
        val result = transformer.transform(contentState())

        // Assert
        val items = result.topBarConfig.items
        val menuTitleIds = items.mapNotNull { (it.title as? TextReference.Res)?.id }
        assertThat(menuTitleIds).containsExactly(
            R.string.tangempay_current_plan_title,
            R.string.tangempay_cashback_title,
            R.string.tangempay_visa_benefits,
            R.string.tangem_pay_terms_limits,
            R.string.tangempay_pay_support,
        ).inOrder()
        val errorEntry = items.first { it.isTitledWith(R.string.tangempay_cashback_title) }
        assertThat(errorEntry.onClick).isSameInstanceAs(onReload)
        assertThat(errorEntry.isEnabled).isTrue()
        assertThat(errorEntry.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_widget_error_description))
        assertThat(result.cashbackBlockState).isNull()
    }

    @Test
    fun `GIVEN reloading WHEN transform THEN loading entry is disabled`() {
        // Arrange
        val transformer = CashbackMenuItemErrorTransformer(onReload = onReload, isReloading = true)

        // Act
        val result = transformer.transform(contentState())

        // Assert
        val loadingEntry = result.topBarConfig.items.first { it.isTitledWith(R.string.tangempay_cashback_title) }
        assertThat(loadingEntry.isEnabled).isFalse()
        assertThat(loadingEntry.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_menu_item_loading))
    }

    @Test
    fun `GIVEN error entry already shown WHEN transform again THEN entry is not duplicated`() {
        // Arrange
        val transformer = CashbackMenuItemErrorTransformer(onReload = onReload, isReloading = false)

        // Act
        val result = transformer.transform(transformer.transform(contentState()))

        // Assert
        val cashbackEntries = result.topBarConfig.items.count { it.isTitledWith(R.string.tangempay_cashback_title) }
        assertThat(cashbackEntries).isEqualTo(1)
    }

    private fun contentState(): TangemPayDetailsUM = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = {},
            onOpenMenu = {},
            items = buildList {
                add(menuItem(R.string.tangempay_current_plan_title))
                add(menuItem(R.string.tangempay_cashback_menu_item_title))
                add(menuItem(R.string.tangempay_visa_benefits))
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