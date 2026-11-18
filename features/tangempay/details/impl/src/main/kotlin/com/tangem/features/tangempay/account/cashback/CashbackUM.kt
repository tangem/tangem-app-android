package com.tangem.features.tangempay.account.cashback

import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.account.TangemPayDetailsUM
import com.tangem.features.tangempay.common.PayMenuItems
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

internal data class CashbackUM(
    val block: CashbackBlockUM? = null,
    val menuItem: TangemPayDropDownItemUM? = null,
)

internal fun TangemPayDetailsUM.withCashback(cashback: CashbackUM): TangemPayDetailsUM = copy(
    cashbackBlockState = cashback.block,
    topBarConfig = topBarConfig.copy(
        items = topBarConfig.items.withMenuItem(cashback.menuItem),
        menu = topBarConfig.menu?.withMenuItem(cashback.menuItem),
    ),
)

private fun ImmutableList<TangemPayDropDownItemUM>.withMenuItem(
    menuItem: TangemPayDropDownItemUM?,
): ImmutableList<TangemPayDropDownItemUM> {
    if (menuItem == null) return this

    val currentPlanIndex = indexOfFirst { it.isTitledWith(R.string.tangempay_current_plan_title) }
    val insertionIndex = if (currentPlanIndex >= 0) currentPlanIndex + 1 else 0
    return toMutableList()
        .apply { add(insertionIndex, menuItem) }
        .toImmutableList()
}

private fun TangemMenu.UiState.withMenuItem(menuItem: TangemPayDropDownItemUM?): TangemMenu.UiState {
    if (menuItem == null) return this

    val currentPlanLabel = resourceReference(R.string.tangempay_current_plan_title)
    val currentPlanIndex = entries.indexOfFirst { (it as? TangemMenu.UiState.Item)?.label == currentPlanLabel }
    val insertionIndex = if (currentPlanIndex >= 0) currentPlanIndex + 1 else 0
    val cashbackEntry = PayMenuItems.cashback(label = menuItem.title, onClick = menuItem.onClick)
    return copy(entries = entries.toMutableList().apply { add(insertionIndex, cashbackEntry) }.toPersistentList())
}

private fun TangemPayDropDownItemUM.isTitledWith(resId: Int): Boolean = (title as? TextReference.Res)?.id == resId