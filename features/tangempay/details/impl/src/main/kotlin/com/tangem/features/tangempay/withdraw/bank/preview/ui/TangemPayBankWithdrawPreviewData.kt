package com.tangem.features.tangempay.withdraw.bank.preview.ui

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.AmountCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.DetailRowUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RecipientCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RouteUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.TransferUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object TangemPayBankWithdrawPreviewData {

    private val header = BankWithdrawHeaderUM(
        title = resourceReference(R.string.tangempay_withdraw_bank_confirm_title),
        subtitle = null,
        onCloseClick = {},
    )

    private val transfer = TransferUM(
        amount = AmountCardUM(
            balance = "$2,130.88",
            amount = "$500",
            icon = CurrencyIconState.Loading,
            symbol = "USDC",
            onClick = {},
        ),
        route = RouteUM(
            rail = BankRail.ACH,
            title = resourceReference(R.string.tangempay_bank_transfer_fee_ach),
            eta = stringReference("1–3 business days"),
            fee = "$1",
        ),
        sendingSummary = stringReference("You are sending $501 including a ACH fee of $1"),
    )

    private val accountCard = persistentListOf(
        DetailRowUM(label = BankWithdrawFormField.AccountNumber.label, value = "0000000000"),
        DetailRowUM(label = BankWithdrawFormField.RoutingNumber.label, value = "021000021"),
    )

    private val personCard = persistentListOf(
        DetailRowUM(label = BankWithdrawFormField.FirstName.label, value = "Saburo"),
        DetailRowUM(label = BankWithdrawFormField.LastName.label, value = "Arasaka"),
        DetailRowUM(label = BankWithdrawFormField.Country.label, value = "United States"),
        DetailRowUM(label = BankWithdrawFormField.Region.label, value = "California"),
        DetailRowUM(label = BankWithdrawFormField.City.label, value = "Night City"),
        DetailRowUM(label = BankWithdrawFormField.AddressLine1.label, value = "Megatower H10, 322F"),
        DetailRowUM(label = BankWithdrawFormField.PostalCode.label, value = "90001"),
    )

    private val businessCard = persistentListOf(
        DetailRowUM(label = BankWithdrawFormField.BusinessName.label, value = "Arasaka Corporation"),
        DetailRowUM(label = BankWithdrawFormField.Country.label, value = "United States"),
        DetailRowUM(label = BankWithdrawFormField.Region.label, value = "California"),
        DetailRowUM(label = BankWithdrawFormField.City.label, value = "Night City"),
        DetailRowUM(label = BankWithdrawFormField.AddressLine1.label, value = "Megatower H10, 322F"),
        DetailRowUM(label = BankWithdrawFormField.PostalCode.label, value = "90001"),
    )

    private val bankCard = persistentListOf(
        DetailRowUM(label = BankWithdrawFormField.BankName.label, value = "Arasaka Bank"),
        DetailRowUM(label = BankWithdrawFormField.BankRegion.label, value = "California"),
        DetailRowUM(label = BankWithdrawFormField.BankCity.label, value = "Night City"),
        DetailRowUM(label = BankWithdrawFormField.BankAddressLine1.label, value = "Ebunike st., 32A"),
        DetailRowUM(label = BankWithdrawFormField.BankPostalCode.label, value = "90002"),
    )

    val summary = TangemPayBankWithdrawPreviewUM.Summary(
        header = header,
        transfer = transfer,
        recipientCards = persistentListOf(
            RecipientCardUM(rows = accountCard, onClick = {}),
            RecipientCardUM(rows = personCard, onClick = {}),
            RecipientCardUM(rows = bankCard, onClick = {}),
        ),
        isSending = false,
        isSendEnabled = true,
        isColdWallet = true,
        onBackClick = {},
        onCloseClick = {},
        onSendClick = {},
    )

    val summaryBusiness = summary.copy(
        recipientCards = persistentListOf(
            RecipientCardUM(rows = accountCard, onClick = {}),
            RecipientCardUM(rows = businessCard, onClick = {}),
            RecipientCardUM(rows = bankCard, onClick = {}),
        ),
    )

    /** A saved recipient's cards are not editable here, so they carry no [RecipientCardUM.onClick]. */
    val summarySavedRecipient = summary.copy(
        recipientCards = summary.recipientCards.map { it.copy(onClick = null) }.toImmutableList(),
    )

    /** A hot wallet signs without a card, so Send drops the Tangem logo. */
    val summaryHotWallet = summary.copy(isColdWallet = false)

    val sending = summary.copy(isSending = true)

    val failedGeneral = TangemPayBankWithdrawPreviewUM.Failed(
        title = resourceReference(R.string.tangempay_withdraw_bank_error_general_title),
        icon = TangemStatusIcon.Kind.Warning,
        onCloseClick = {},
        onRetryClick = {},
    )

    val failedUnavailable = TangemPayBankWithdrawPreviewUM.Failed(
        title = resourceReference(R.string.tangempay_withdraw_bank_error_recipient_region),
        icon = TangemStatusIcon.Kind.HeartBroken,
        onCloseClick = {},
        onRetryClick = null,
    )
}