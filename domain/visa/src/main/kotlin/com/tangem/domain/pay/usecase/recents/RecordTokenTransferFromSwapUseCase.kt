package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore

/**
 * Records the crypto portfolio side of an exchange with the payment account: a withdrawal writes the TO side, a
 * top-up the FROM one. A pair with no payment account on either side, or whose sides belong to different
 * wallets, is none of the journal's business.
 */
class RecordTokenTransferFromSwapUseCase(
    private val recentsStore: PaymentAccountRecentsStore,
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

        when {
            fromAccount is Account.Payment && toAccount is Account.CryptoPortfolio ->
                markUsed(userWalletId = toUserWalletId, account = toAccount, currency = toCurrency)
            toAccount is Account.Payment && fromAccount is Account.CryptoPortfolio ->
                markUsed(userWalletId = fromUserWalletId, account = fromAccount, currency = fromCurrency)
        }
    }

    private suspend fun markUsed(
        userWalletId: UserWalletId,
        account: Account.CryptoPortfolio,
        currency: CryptoCurrency,
    ) {
        recentsStore.markUsed(userWalletId = userWalletId, accountId = account.accountId, currencyId = currency.id)
    }
}