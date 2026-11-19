package com.tangem.features.tangempay.withdraw.bank.form.model

import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.withdraw.toShortName

/**
 * Saved recipients of the typed number's rail — SPEI for a CLABE, the US rails for an account number — whose number
 * starts with what the user has typed, sorted by the name the suggestion shows; a blank prefix suggests nothing, and
 * a fully typed number still suggests its own account.
 */
internal object BankAccountSuggestions {

    fun match(accounts: List<BankAccount>, field: BankWithdrawFormField, prefix: String): List<BankAccount> {
        val needle = prefix.trim()
        if (needle.isEmpty()) return emptyList()
        return accounts
            .filter { account -> field.suggests(account) }
            .filter { account -> account.details.accountNumber.startsWith(needle) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.details.beneficiary.toShortName() })
    }

    private fun BankWithdrawFormField.suggests(account: BankAccount): Boolean = when (this) {
        BankWithdrawFormField.Clabe -> account.details.rail == BankRail.SPEI
        BankWithdrawFormField.AccountNumber -> account.details.rail != BankRail.SPEI
        else -> false
    }
}

internal val BankWithdrawFormField.canSuggestSavedRecipients: Boolean
    get() = this == BankWithdrawFormField.AccountNumber || this == BankWithdrawFormField.Clabe