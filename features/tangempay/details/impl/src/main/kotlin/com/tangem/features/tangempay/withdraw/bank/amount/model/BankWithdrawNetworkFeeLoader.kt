package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import java.math.BigDecimal
import javax.inject.Inject

internal class BankWithdrawNetworkFeeLoader @Inject constructor(
    private val moneyAccountTransfer: MoneyAccountTransfer,
) {

    suspend fun load(account: BankWithdrawSourceAccount, token: CryptoCurrency.Token): Either<GetFeeError, BigDecimal> =
        when (account) {
            is BankWithdrawSourceAccount.Payment -> BigDecimal.ZERO.right()
            is BankWithdrawSourceAccount.Money -> moneyAccountTransfer.estimateFee(account.userWalletId, token)
        }
}