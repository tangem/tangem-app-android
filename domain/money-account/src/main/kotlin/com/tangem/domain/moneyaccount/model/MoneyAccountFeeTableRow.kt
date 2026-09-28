package com.tangem.domain.moneyaccount.model

import com.tangem.domain.pay.model.AccountFee

data class MoneyAccountFeeTableRow(
    val paymentMethod: MoneyAccountPaymentMethod,
    val receiveFee: AccountFee?,
    val sendFee: AccountFee?,
)