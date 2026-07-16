package com.tangem.domain.pay.usecase

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

class GetPaymentAccountCryptoCurrencyStatusUseCase(
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<Pair<Account.Payment, CryptoCurrencyStatus>> {
        return invoke(userWalletId).mapNotNull { (accountStatus, cryptoCurrencyStatus) ->
            if (cryptoCurrencyStatus.currency == cryptoCurrency) {
                accountStatus.account to cryptoCurrencyStatus
            } else {
                null
            }
        }
    }

    operator fun invoke(userWalletId: UserWalletId): Flow<Pair<AccountStatus.Payment, CryptoCurrencyStatus>> {
        return paymentAccountStatusSupplier(userWalletId).mapNotNull { accountStatus ->
            val cryptoCurrencyStatus = when (val statusValue = accountStatus.value) {
                is PaymentAccountStatusValue.Loaded -> statusValue.cryptoCurrencyStatus
                else -> return@mapNotNull null
            }
            accountStatus to cryptoCurrencyStatus
        }
    }

    suspend fun invokeSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Option<Pair<Account.Payment, CryptoCurrencyStatus>> {
        val (accountStatus, cryptoCurrencyStatus) = invokeSync(userWalletId)
            .getOrNull() ?: return none()
        return if (cryptoCurrencyStatus.currency == cryptoCurrency) {
            (accountStatus.account to cryptoCurrencyStatus).some()
        } else {
            none()
        }
    }

    suspend fun invokeSync(userWalletId: UserWalletId): Option<Pair<AccountStatus.Payment, CryptoCurrencyStatus>> {
        val accountStatus = paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull() ?: return none()
        val cryptoCurrencyStatus = when (val statusValue = accountStatus.value) {
            is PaymentAccountStatusValue.Loaded -> statusValue.cryptoCurrencyStatus
            else -> return none()
        }
        return (accountStatus to cryptoCurrencyStatus).some()
    }
}