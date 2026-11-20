package com.tangem.features.tangempay.withdraw.bank.form.model

import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.withdraw.toShortName

/**
 * Saved US recipients whose account number starts with what the user has typed, sorted by the name the suggestion
 * shows; a blank prefix suggests nothing, and a fully typed number still suggests its own account.
 */
internal object BankAccountSuggestions {

    fun match(accounts: List<BankAccount>, prefix: String): List<BankAccount> {
        val needle = prefix.trim()
        if (needle.isEmpty()) return emptyList()
        return accounts
            .filter { account -> account.details.rail != BankRail.SPEI }
            .filter { account -> account.details.accountNumber.startsWith(needle) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.details.beneficiary.toShortName() })
    }
}