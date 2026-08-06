package com.tangem.features.tangempay.txhistory.details

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.ExclusionReason
import com.tangem.domain.visa.model.TangemPayTxHistoryItem.Cashback.Status
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.StringsSigns
import java.math.BigDecimal
import java.util.Currency

/**
 * Builds the transaction-detail "Cashback" row from the independently-loaded cashback and its load
 * state. Only shown for spend transactions when the cashback feature is enabled.
 */
internal object TangemPayCashbackDetailUmConverter {

    private val defaultCurrency: Currency = Currency.getInstance("USD")

    fun convert(
        transaction: TangemPayTxHistoryItem,
        cashback: TangemPayTxHistoryItem.Cashback?,
        loadState: TransactionLoadState,
        isCashbackEnabled: Boolean,
        onRefreshClick: () -> Unit,
    ): CashbackDetailUM? {
        if (!isCashbackEnabled) return null
        if (transaction !is TangemPayTxHistoryItem.Spend) return null
        return when (loadState) {
            TransactionLoadState.Loading -> CashbackDetailUM.Loading
            TransactionLoadState.Error -> CashbackDetailUM.Error(onRefreshClick = onRefreshClick)
            TransactionLoadState.Loaded -> cashback?.toUm()
        }
    }

    private fun TangemPayTxHistoryItem.Cashback.toUm(): CashbackDetailUM? {
        return when (status) {
            Status.AWAITING_CALCULATION -> CashbackDetailUM.AwaitingCalculation
            Status.CONFIRMED,
            Status.ESTIMATED,
            -> {
                val amount = amount ?: return null
                CashbackDetailUM.Content(
                    value = stringReference(formatAmount(amount, currency)),
                    subvalue = earned(amount),
                )
            }
            Status.EXCLUDED -> CashbackDetailUM.Content(
                value = resourceReference(R.string.tangem_pay_transaction_details_cashback_none),
                subvalue = excluded(),
            )
            Status.UNKNOWN -> null
        }
    }

    private fun TangemPayTxHistoryItem.Cashback.earned(amount: BigDecimal): TextReference? = when {
        isCapTrimmed -> resourceReference(R.string.tangem_pay_transaction_details_cashback_cap_reached)
        amount.signum() < 0 -> resourceReference(R.string.tangem_pay_transaction_details_cashback_refund)
        else -> null
    }

    private fun TangemPayTxHistoryItem.Cashback.excluded(): TextReference? = when (exclusionReason) {
        ExclusionReason.MCC_EXCLUDED ->
            resourceReference(R.string.tangem_pay_transaction_details_cashback_mcc_excluded)
        ExclusionReason.MONTHLY_CAP_REACHED ->
            resourceReference(R.string.tangem_pay_transaction_details_cashback_cap_reached)
        ExclusionReason.MERCHANT_COUNTRY_EXCLUDED ->
            resourceReference(R.string.tangem_pay_transaction_details_cashback_region_excluded)
        ExclusionReason.BELOW_MIN -> stringReference("Below minimum")
        ExclusionReason.CUSTOMER_BLOCKLISTED,
        ExclusionReason.UNKNOWN,
        null,
        -> null
    }

    private fun formatAmount(amount: BigDecimal, currency: Currency?): String {
        val fiatCurrency = currency ?: defaultCurrency
        val prefix = if (amount.signum() < 0) StringsSigns.MINUS else StringsSigns.PLUS
        return prefix + amount.abs().format {
            fiat(fiatCurrencyCode = fiatCurrency.currencyCode, fiatCurrencySymbol = fiatCurrency.symbol)
        }
    }
}