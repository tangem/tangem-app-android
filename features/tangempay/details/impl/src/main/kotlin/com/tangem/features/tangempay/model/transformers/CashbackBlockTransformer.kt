package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.entity.CashbackBlockUM
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

/**
 * Resolves the Payment account cashback block from a [CashbackSummary]:
 * - [CashbackSummary.Enabled] -> tappable [CashbackBlockUM.Widget];
 * - [CashbackSummary.Deactivated] (unless dismissed) -> [CashbackBlockUM.DeactivatedBanner];
 * - otherwise -> hidden (`null`).
 */
internal class CashbackBlockTransformer(
    private val summary: CashbackSummary,
    private val isDeactivationDismissed: Boolean,
    private val dateFormatter: TangemPayCashbackDateFormatter,
    private val onClick: () -> Unit,
    private val onGotIt: () -> Unit,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val block: CashbackBlockUM? = when (summary) {
            is CashbackSummary.Enabled -> buildWidget(summary)
            CashbackSummary.Deactivated -> if (isDeactivationDismissed) {
                null
            } else {
                CashbackBlockUM.DeactivatedBanner(onGotIt = onGotIt)
            }
            CashbackSummary.Disabled,
            CashbackSummary.Unknown,
            -> null
        }
        return prevState.copy(cashbackBlockState = block)
    }

    private fun buildWidget(enabled: CashbackSummary.Enabled): CashbackBlockUM.Widget {
        val cashback = enabled.cashback
        val currency = getJavaCurrencyByCode(cashback.currency)
        val amount = cashback.confirmedAmount.format {
            fiat(currency.currencyCode, currency.symbol).defaultAmount()
        }
        val month = dateFormatter.formatMonth(cashback.period.year, cashback.period.month)
        val window = dateFormatter.formatWindow(cashback.period.payoutStart, cashback.period.payoutEnd)
        return CashbackBlockUM.Widget(
            title = resourceReference(R.string.tangempay_cashback_widget_title, wrappedList(amount, month)),
            subtitle = resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(window)),
            onClick = onClick,
        )
    }
}