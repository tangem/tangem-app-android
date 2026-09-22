package com.tangem.features.tangempay.withdraw.bank.recipients.model

import com.tangem.domain.pay.model.bankofframp.BankAccount

/** Saved recipients once loaded (null while loading) plus the search field input. */
internal data class RecipientsState(
    val accounts: List<BankAccount>? = null,
    val query: String = "",
    val isSearchActive: Boolean = false,
)