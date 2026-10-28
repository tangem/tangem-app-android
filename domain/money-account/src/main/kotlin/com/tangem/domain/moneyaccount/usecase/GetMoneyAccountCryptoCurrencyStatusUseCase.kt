package com.tangem.domain.moneyaccount.usecase

import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

class GetMoneyAccountCryptoCurrencyStatusUseCase(
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Flow<Pair<Account.Money, CryptoCurrencyStatus>> {
        return moneyAccountStatusSupplier(userWalletId).mapNotNull { accountStatus ->
            accountStatus.findCurrencyStatus(currency)
        }
    }

    suspend fun invokeSync(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Option<Pair<Account.Money, CryptoCurrencyStatus>> {
        val accountStatus = moneyAccountStatusSupplier(userWalletId).firstOrNull()
        return accountStatus?.findCurrencyStatus(currency).toOption()
    }

    private fun AccountStatus.Money.findCurrencyStatus(
        currency: CryptoCurrency,
    ): Pair<Account.Money, CryptoCurrencyStatus>? {
        val accountValue = value as? MoneyAccountStatusValue.Account ?: return null
        if (accountValue.cryptoCurrency.id != currency.id) return null
        val status = accountValue.cryptoCurrencyStatus ?: return null
        return account to status
    }
}