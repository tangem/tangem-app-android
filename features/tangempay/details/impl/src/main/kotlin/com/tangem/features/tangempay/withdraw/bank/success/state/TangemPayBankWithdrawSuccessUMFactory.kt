package com.tangem.features.tangempay.withdraw.bank.success.state

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.formatFiat
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.recipientDetails
import com.tangem.features.tangempay.withdraw.bank.success.state.TangemPayBankWithdrawSuccessUM.RowUM
import com.tangem.features.tangempay.withdraw.toFullName
import kotlinx.collections.immutable.toImmutableList
import org.joda.time.DateTime
import java.util.Currency

internal object TangemPayBankWithdrawSuccessUMFactory {

    /** [shownAt] is when the transfer was handed over: the order result carries no date of its own. */
    fun create(draft: BankWithdrawDraft, shownAt: Long, onCloseClick: () -> Unit): TangemPayBankWithdrawSuccessUM {
        val details = draft.recipientDetails()
        val amount = draft.sentAmount()
        return TangemPayBankWithdrawSuccessUM(
            title = if (amount == null) {
                resourceReference(R.string.common_sending)
            } else {
                resourceReference(R.string.tangempay_withdraw_bank_success_title, wrappedList(amount))
            },
            subtitle = stringReference(
                DateTimeFormatters.formatDate(DateTime(shownAt), DateTimeFormatters.dateTimeMMMdYYYY),
            ),
            rows = listOfNotNull(
                row(R.string.send_recipient, draft.recipient?.displayName ?: details?.beneficiary?.toFullName()),
                row(R.string.tangempay_withdraw_bank_field_bank_name, details?.bankName),
                row(R.string.tangempay_withdraw_bank_field_account_number, details?.accountNumber),
            ).toImmutableList(),
            onCloseClick = onCloseClick,
        )
    }

    private fun row(@StringRes labelRes: Int, value: String?): RowUM? = value
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { RowUM(label = resourceReference(labelRes), value = it) }

    private fun BankWithdrawDraft.sentAmount(): String? {
        val amount = amount ?: return null
        val currencyCode = destination?.fee?.currency ?: return null
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull() ?: return null
        return amount.formatFiat(currency)
    }
}