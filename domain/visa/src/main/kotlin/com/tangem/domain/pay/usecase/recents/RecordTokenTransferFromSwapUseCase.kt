package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.recents.TokenTransferJournal
import com.tangem.domain.pay.repository.AccountRecentsStore

/**
 * Records the crypto portfolio side of an exchange with the payment or Money account, in that account's journal: a
 * withdrawal writes the TO side, a top-up the FROM one. A pair with neither account on either side, or whose sides
 * belong to different wallets, is none of the journals' business.
 */
class RecordTokenTransferFromSwapUseCase(
    private val recentsStore: AccountRecentsStore,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        fromUserWalletId: UserWalletId,
        fromAccount: Account?,
        fromCurrency: CryptoCurrency,
        toUserWalletId: UserWalletId,
        toAccount: Account?,
        toCurrency: CryptoCurrency,
    ) {
        if (fromUserWalletId != toUserWalletId) return

        val fromJournal = TokenTransferJournal.of(fromAccount)
        val toJournal = TokenTransferJournal.of(toAccount)
        when {
            fromJournal != null && toAccount is Account.CryptoPortfolio ->
                markUsed(
                    userWalletId = toUserWalletId,
                    journal = fromJournal,
                    account = toAccount,
                    currency = toCurrency,
                )
            toJournal != null && fromAccount is Account.CryptoPortfolio ->
                markUsed(
                    userWalletId = fromUserWalletId,
                    journal = toJournal,
                    account = fromAccount,
                    currency = fromCurrency,
                )
        }
    }

    private suspend fun markUsed(
        userWalletId: UserWalletId,
        journal: TokenTransferJournal,
        account: Account.CryptoPortfolio,
        currency: CryptoCurrency,
    ) {
        recentsStore.markUsed(
            userWalletId = userWalletId,
            journal = journal,
            accountId = account.accountId,
            currencyId = currency.id,
        )
    }
}