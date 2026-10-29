package com.tangem.domain.pay.model.recents

import com.tangem.domain.models.account.Account

enum class TokenTransferJournal {
    PAYMENT_ACCOUNT,
    MONEY_ACCOUNT,
    ;

    companion object {

        fun of(account: Account?): TokenTransferJournal? = when (account) {
            is Account.Payment -> PAYMENT_ACCOUNT
            is Account.Money -> MONEY_ACCOUNT
            else -> null
        }
    }
}