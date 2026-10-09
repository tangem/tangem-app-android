package com.tangem.features.tangempay.account.cashback

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.account.CashbackBlockUM
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.utils.extensions.isNegative

internal class CashbackUMFactory(
    private val dateFormatter: TangemPayCashbackDateFormatter,
    private val onWidgetClick: () -> Unit,
    private val onMenuItemClick: () -> Unit,
    private val onGotIt: () -> Unit,
    private val onReload: () -> Unit,
) {

    fun create(content: CashbackContent): CashbackUM = when (content) {
        CashbackContent.Hidden -> CashbackUM()
        is CashbackContent.Widget -> CashbackUM(block = createWidget(content.cashback))
        is CashbackContent.MenuEntry -> CashbackUM(menuItem = createMenuItem(content.cashback))
        CashbackContent.DeactivationBanner -> CashbackUM(block = CashbackBlockUM.DeactivatedBanner(onGotIt = onGotIt))
        is CashbackContent.Unavailable -> CashbackUM(
            block = CashbackBlockUM.Error(onReload = onReload, isReloading = content.isRetrying),
        )
    }

    private fun createWidget(cashback: TangemPayCashback): CashbackBlockUM.Widget {
        val currency = getJavaCurrencyByCode(cashback.currency)
        val amount = cashback.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).defaultAmount() }
        return CashbackBlockUM.Widget(
            title = resourceReference(R.string.tangempay_cashback_widget_title, wrappedList(amount, cashback.month())),
            isNegative = cashback.confirmedAmount.isNegative(),
            onClick = onWidgetClick,
        )
    }

    private fun createMenuItem(cashback: TangemPayCashback): TangemPayDropDownItemUM = TangemPayDropDownItemUM(
        title = resourceReference(R.string.tangempay_cashback_menu_item_title, wrappedList(cashback.month())),
        onClick = onMenuItemClick,
        icon = TangemIconUM.Icon(
            imageVector = Icons.ic_percent_backward_20,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
    )

    private fun TangemPayCashback.month(): String = dateFormatter.formatMonth(period.year, period.month)
}