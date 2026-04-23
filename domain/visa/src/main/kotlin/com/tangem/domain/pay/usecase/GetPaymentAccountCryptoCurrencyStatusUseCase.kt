package com.tangem.domain.pay.usecase

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.flow.firstOrNull

class GetPaymentAccountCryptoCurrencyStatusUseCase(
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Option<Pair<Account.Payment, CryptoCurrencyStatus>> {
        val accountStatus = paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull() ?: return none()
        val cryptoCurrencyStatus = when (val statusValue = accountStatus.value) {
            is PaymentAccountStatusValue.Loaded -> statusValue.cryptoCurrencyStatus
            else -> return none()
        }
        return if (cryptoCurrencyStatus.currency == cryptoCurrency) {
            (accountStatus.account to cryptoCurrencyStatus).some()
        } else {
            none()
        }
    }
}