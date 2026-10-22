package com.tangem.features.tangempay.withdraw.bank

import com.tangem.domain.models.wallet.UserWalletId

internal sealed interface BankWithdrawSourceAccount {

    val userWalletId: UserWalletId

    data class Payment(override val userWalletId: UserWalletId) : BankWithdrawSourceAccount

    data class Money(
        override val userWalletId: UserWalletId,
        val productInstanceId: String,
    ) : BankWithdrawSourceAccount
}