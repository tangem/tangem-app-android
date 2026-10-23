package com.tangem.domain.pay.repository

import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.recents.TokenTransferJournal

/**
 * Local usage log of the account + token pairs the payment and Money accounts have exchanged with: nothing on the
 * backend records them, so the app keeps its own journal for each of them.
 */
interface PaymentAccountRecentsStore {

    suspend fun markUsed(
        userWalletId: UserWalletId,
        journal: TokenTransferJournal,
        accountId: AccountId,
        currencyId: CryptoCurrency.ID,
    )

    /** account + currency → epoch millis of the last use. */
    suspend fun getUsage(userWalletId: UserWalletId, journal: TokenTransferJournal): Map<AccountCurrencyId, Long>
}