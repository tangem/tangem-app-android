package com.tangem.features.tangempay.withdraw.bank.preview.state

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.amount.state.formatFiat
import com.tangem.features.tangempay.withdraw.bank.amount.state.toText
import com.tangem.features.tangempay.withdraw.bank.amount.state.toTextReference
import com.tangem.features.tangempay.withdraw.bank.amount.state.toTitleReference
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.model.recipientDetails
import com.tangem.features.tangempay.withdraw.bank.model.toFormValues
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewState
import com.tangem.features.tangempay.withdraw.bank.preview.model.toOrderRequest
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.AmountCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.DetailRowUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RecipientCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RouteUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.TransferUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.Currency

private val ACCOUNT_FIELDS = setOf(BankWithdrawFormField.AccountNumber, BankWithdrawFormField.RoutingNumber)

internal class TangemPayBankWithdrawPreviewUMFactory(private val callbacks: Callbacks) {

    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    fun create(draft: BankWithdrawDraft, state: PreviewState): TangemPayBankWithdrawPreviewUM {
        val failure = state.failure
        if (failure != null) return createFailed(failure)
        return TangemPayBankWithdrawPreviewUM.Summary(
            header = BankWithdrawHeaderFactory.create(draft = draft, step = TangemPayBankWithdrawInnerRoute.Preview),
            transfer = createTransfer(draft = draft, isSending = state.isSending),
            recipientCards = draft.recipientDetails()?.let { details ->
                createRecipientCards(details = details, isEditable = draft.recipient == null && !state.isSending)
            } ?: persistentListOf(),
            isSending = state.isSending,
            isSendEnabled = draft.toOrderRequest() != null,
            onBackClick = callbacks.onBackClick,
            onCloseClick = callbacks.onCloseClick,
            onSendClick = callbacks.onSendClick,
        )
    }

    private fun createTransfer(draft: BankWithdrawDraft, isSending: Boolean): TransferUM? {
        val amount = draft.amount ?: return null
        val source = draft.source ?: return null
        val destination = draft.destination ?: return null
        val currency = Currency.getInstance(destination.fee.currency)
        val railTitle = destination.rail.toTitleReference()
        val fee = destination.fee.toText()
        return TransferUM(
            amount = AmountCardUM(
                balance = source.fiatBalance.formatFiat(currency),
                amount = amount.formatFiat(currency),
                icon = iconConverter.convert(source.token),
                symbol = source.token.symbol,
                onClick = if (isSending) {
                    null
                } else {
                    { callbacks.onEditClick(BankWithdrawEditTarget.Amount) }
                },
            ),
            route = RouteUM(
                rail = destination.rail,
                title = railTitle,
                eta = destination.eta.toTextReference(),
                fee = fee,
            ),
            sendingSummary = resourceReference(
                R.string.tangempay_withdraw_bank_sending_summary,
                wrappedList((amount + destination.fee.amount).formatFiat(currency), railTitle, fee),
            ),
        )
    }

    private fun createRecipientCards(
        details: BankAccountDetails,
        isEditable: Boolean,
    ): ImmutableList<RecipientCardUM> {
        val values = details.toFormValues()
        val recipientFields = BankWithdrawFormFields.recipientDetails(
            isBusiness = details.beneficiary is Beneficiary.Business,
        )
        val cards = listOf(
            recipientFields.filter { it in ACCOUNT_FIELDS } to BankWithdrawEditTarget.RecipientDetails,
            recipientFields.filterNot { it in ACCOUNT_FIELDS } to BankWithdrawEditTarget.RecipientDetails,
            BankWithdrawFormFields.bankDetails to BankWithdrawEditTarget.BankDetails,
        )
        return cards.map { (fields, target) ->
            RecipientCardUM(
                rows = fields.mapNotNull { field ->
                    // A country without regions leaves its region empty, and an empty row reads as missing data.
                    values[field]
                        ?.takeIf(String::isNotBlank)
                        ?.let { value -> DetailRowUM(label = field.label, value = value) }
                }.toImmutableList(),
                onClick = if (isEditable) {
                    { callbacks.onEditClick(target) }
                } else {
                    null
                },
            )
        }.toImmutableList()
    }

    private fun createFailed(failure: PreviewFailure): TangemPayBankWithdrawPreviewUM.Failed = when (failure) {
        PreviewFailure.RecipientRegion -> unavailable(R.string.tangempay_withdraw_bank_error_recipient_region)
        PreviewFailure.SenderRegion -> unavailable(R.string.tangempay_withdraw_bank_error_sender_region)
        PreviewFailure.Business -> unavailable(R.string.tangempay_withdraw_bank_error_business)
        PreviewFailure.General -> TangemPayBankWithdrawPreviewUM.Failed(
            title = resourceReference(R.string.tangempay_withdraw_bank_error_general_title),
            onCloseClick = callbacks.onCloseClick,
            onRetryClick = callbacks.onRetryClick,
        )
    }

    private fun unavailable(@StringRes titleRes: Int) = TangemPayBankWithdrawPreviewUM.Failed(
        title = resourceReference(titleRes),
        onCloseClick = callbacks.onCloseClick,
        onRetryClick = null,
    )

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onSendClick: () -> Unit,
        val onRetryClick: () -> Unit,
        val onEditClick: (BankWithdrawEditTarget) -> Unit,
    )
}