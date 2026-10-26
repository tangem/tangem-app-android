package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount

internal data class BankWithdrawFromUM(
    val title: TextReference,
    val icon: AccountIconUM,
)

internal object BankWithdrawFromFactory {

    fun create(account: BankWithdrawSourceAccount): BankWithdrawFromUM = when (account) {
        is BankWithdrawSourceAccount.Payment -> BankWithdrawFromUM(
            title = resourceReference(R.string.tangempay_payment_account),
            icon = AccountIconUM.Payment,
        )
        is BankWithdrawSourceAccount.Money -> BankWithdrawFromUM(
            title = resourceReference(R.string.money_account_main_name),
            icon = AccountIconUM.Money,
        )
    }
}