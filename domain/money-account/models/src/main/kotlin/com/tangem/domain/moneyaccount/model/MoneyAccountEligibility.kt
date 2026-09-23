package com.tangem.domain.moneyaccount.model

import com.tangem.domain.models.wallet.UserWallet

sealed interface MoneyAccountEligibility {

    data class Available(
        val wallets: List<UserWallet>,
    ) : MoneyAccountEligibility

    data object NotAvailable : MoneyAccountEligibility
}