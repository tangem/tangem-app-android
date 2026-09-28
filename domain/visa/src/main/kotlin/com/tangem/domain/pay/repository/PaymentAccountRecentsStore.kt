package com.tangem.domain.pay.repository

import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Local usage log of the account + token pairs the payment account has exchanged with: nothing on the backend
 * records them, so the app keeps its own journal.
 */
interface PaymentAccountRecentsStore {

    suspend fun markUsed(userWalletId: UserWalletId, accountId: AccountId, currencyId: CryptoCurrency.ID)

    /** account + currency → epoch millis of the last use. */
    suspend fun getUsage(userWalletId: UserWalletId): Map<AccountCurrencyId, Long>
}