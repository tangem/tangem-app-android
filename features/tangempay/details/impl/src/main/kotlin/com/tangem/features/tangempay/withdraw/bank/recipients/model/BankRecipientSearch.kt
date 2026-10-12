package com.tangem.features.tangempay.withdraw.bank.recipients.model

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.withdraw.toFullName

/** Client-side search over saved recipients by the name shown or account number; a blank query keeps every account. */
internal object BankRecipientSearch {

    fun filter(accounts: List<BankAccount>, query: String): List<BankAccount> {
        val needle = query.trim()
        if (needle.isEmpty()) return accounts
        return accounts.filter { account ->
            account.details.beneficiary.toFullName().contains(needle, ignoreCase = true) ||
                account.details.accountNumber.contains(needle)
        }
    }
}