package com.tangem.features.tangempay.account

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.utils.extensions.isNegative
import com.tangem.utils.extensions.isZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Resolves all Payment account cashback UI from a [CashbackSummary] in one place.
 *
 * Cashback block:
 * - [CashbackSummary.Enabled] with [CashbackDisplayMode.FULL] -> tappable [CashbackBlockUM.Widget];
 * - [CashbackSummary.Deactivated] (unless dismissed) -> [CashbackBlockUM.DeactivatedBanner];
 * - otherwise -> hidden (`null`).
 *
 * Top bar menu entry:
 * - [CashbackSummary.Enabled] with [CashbackDisplayMode.ALT_BLOCK] (EU/EEA) -> "Cashback in %month%"
 *   item inserted below the "Current plan" entry (above "Terms and fees" while the plan entry is
 *   absent); otherwise the entry is removed. A menu with neither anchor (e.g. the deactivated
 *   account menu) never receives the entry.
 */
internal class CashbackBlockTransformer(
    private val summary: CashbackSummary,
    private val isDeactivationDismissed: Boolean,
    private val dateFormatter: TangemPayCashbackDateFormatter,
    private val onClick: () -> Unit,
    private val onMenuItemClick: () -> Unit,
    private val onGotIt: () -> Unit,
) : Transformer<TangemPayDetailsUM> {

    val isAltBlockMode: Boolean
        get() = (summary as? CashbackSummary.Enabled)?.displayMode == CashbackDisplayMode.ALT_BLOCK

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val block: CashbackBlockUM? = when (summary) {
            is CashbackSummary.Enabled -> when (summary.displayMode) {
                CashbackDisplayMode.FULL -> buildWidget(summary)
                CashbackDisplayMode.ALT_BLOCK -> null
            }
            CashbackSummary.Deactivated -> if (isDeactivationDismissed) {
                null
            } else {
                CashbackBlockUM.DeactivatedBanner(onGotIt = onGotIt)
            }
            CashbackSummary.Disabled,
            CashbackSummary.Unknown,
            -> null
        }
        val menuItem = (summary as? CashbackSummary.Enabled)
            ?.takeIf { it.displayMode == CashbackDisplayMode.ALT_BLOCK }
            ?.let(::buildMenuItem)
        return prevState.copy(
            cashbackBlockState = block,
            topBarConfig = prevState.topBarConfig.copy(
                items = prevState.topBarConfig.items.withCashbackMenuItem(menuItem),
            ),
        )
    }

    private fun buildWidget(enabled: CashbackSummary.Enabled): CashbackBlockUM.Widget {
        val cashback = enabled.cashback
        val currency = getJavaCurrencyByCode(cashback.currency)
        val amount = cashback.confirmedAmount.format {
            fiat(currency.currencyCode, currency.symbol).defaultAmount()
        }
        val month = dateFormatter.formatMonth(cashback.period.year, cashback.period.month)
        val isNegative = cashback.confirmedAmount.isNegative()
        val subtitle = when {
            isNegative -> resourceReference(R.string.tangempay_cashback_refund_banner)
            cashback.confirmedAmount.isZero() -> resourceReference(
                R.string.tangempay_cashback_widget_empty_description,
            )
            else -> {
                val window = dateFormatter.formatWindow(cashback.period.payoutStart, cashback.period.payoutEnd)
                window?.let { resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(it)) }
            }
        }
        return CashbackBlockUM.Widget(
            title = resourceReference(R.string.tangempay_cashback_widget_title, wrappedList(amount, month)),
            subtitle = subtitle,
            onClick = onClick,
            isNegative = isNegative,
        )
    }

    private fun buildMenuItem(enabled: CashbackSummary.Enabled): TangemPayDropDownItemUM {
        val month = dateFormatter.formatMonth(enabled.cashback.period.year, enabled.cashback.period.month)
        return TangemPayDropDownItemUM(
            title = resourceReference(R.string.tangempay_cashback_menu_item_title, wrappedList(month)),
            onClick = onMenuItemClick,
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_percent_backward_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}

/**
 * Removes any cashback entry (the regular "Cashback in %month%" one and the error/loading
 * "Cashback" one) and, when [menuItem] is not `null`, inserts it below the "Current plan" entry.
 * While the plan entry is absent (tariff plan not loaded or the toggle is off), the entry goes
 * above "Terms and fees" instead. A menu with neither anchor never receives the entry.
 */
internal fun ImmutableList<TangemPayDropDownItemUM>.withCashbackMenuItem(
    menuItem: TangemPayDropDownItemUM?,
): ImmutableList<TangemPayDropDownItemUM> {
    val cleared = filterNot { item ->
        item.isTitledWith(R.string.tangempay_cashback_menu_item_title) ||
            item.isTitledWith(R.string.tangempay_cashback_title)
    }
    val currentPlanIndex = cleared.indexOfFirst { it.isTitledWith(R.string.tangempay_current_plan_title) }
    val insertionIndex = if (currentPlanIndex >= 0) {
        currentPlanIndex + 1
    } else {
        cleared.indexOfFirst { it.isTitledWith(R.string.tangem_pay_terms_limits) }
    }
    return if (menuItem == null || insertionIndex < 0) {
        cleared.toImmutableList()
    } else {
        cleared.toMutableList()
            .apply { add(insertionIndex, menuItem) }
            .toImmutableList()
    }
}

internal fun TangemPayDropDownItemUM.isTitledWith(resId: Int): Boolean {
    return (title as? TextReference.Res)?.id == resId
}