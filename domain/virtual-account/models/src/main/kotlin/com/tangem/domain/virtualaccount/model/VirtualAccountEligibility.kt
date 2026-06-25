package com.tangem.domain.virtualaccount.model

import com.tangem.domain.models.wallet.UserWallet

sealed interface VirtualAccountEligibility {

    data class Available(
        val wallets: List<UserWallet>,
    ) : VirtualAccountEligibility

    data object NotAvailable : VirtualAccountEligibility
}