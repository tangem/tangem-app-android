package com.tangem.features.tangempay.withdraw.bank.recipients.state

import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.recipients.model.BankRecipientSearch
import com.tangem.features.tangempay.withdraw.bank.recipients.model.RecipientsState
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.ContentUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.RecipientUM
import com.tangem.features.tangempay.withdraw.toFullName
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayBankWithdrawRecipientsUMFactory(private val callbacks: Callbacks) {

    fun create(state: RecipientsState): TangemPayBankWithdrawRecipientsUM = TangemPayBankWithdrawRecipientsUM(
        search = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = state.query,
            onQueryChange = callbacks.onQueryChange,
            isActive = state.isSearchActive,
            onActiveChange = callbacks.onSearchActiveChange,
            onCloseClick = callbacks.onSearchCloseClick,
        ),
        content = createContent(state),
        onBackClick = callbacks.onBackClick,
        onCloseClick = callbacks.onCloseClick,
    )

    private fun createContent(state: RecipientsState): ContentUM {
        val accounts = state.accounts ?: return ContentUM.Loading
        val matched = BankRecipientSearch.filter(accounts = accounts, query = state.query)
        if (matched.isEmpty()) return ContentUM.NoResults
        val recipients = matched
            .map(::toRecipient)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, RecipientUM::name))
        return ContentUM.Recipients(items = recipients.toImmutableList())
    }

    private fun toRecipient(account: BankAccount): RecipientUM {
        val name = account.details.beneficiary.toFullName()
        return RecipientUM(
            id = account.id,
            initial = name.take(1).uppercase(),
            name = name,
            subtitle = createSubtitle(account.details),
            onClick = { callbacks.onRecipientClick(account) },
        )
    }

    private fun createSubtitle(details: BankAccountDetails): TextReference = when (details.beneficiary) {
        is Beneficiary.Individual -> stringReference(details.accountNumber)
        is Beneficiary.Business -> combinedReference(
            stringReference("${details.accountNumber} · "),
            resourceReference(R.string.tangempay_withdraw_bank_recipient_business),
        )
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onQueryChange: (String) -> Unit,
        val onSearchActiveChange: (Boolean) -> Unit,
        val onSearchCloseClick: () -> Unit,
        val onRecipientClick: (BankAccount) -> Unit,
    )
}