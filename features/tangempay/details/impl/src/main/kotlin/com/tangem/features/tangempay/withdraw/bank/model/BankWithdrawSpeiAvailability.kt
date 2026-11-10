package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import javax.inject.Inject

internal class BankWithdrawSpeiAvailability @Inject constructor(
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) {

    fun isOffered(account: BankWithdrawSourceAccount): Boolean =
        account is BankWithdrawSourceAccount.Money && moneyAccountFeatureToggles.isMxnEnabled
}