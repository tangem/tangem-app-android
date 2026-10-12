package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.recents.RecentTokenTransfer
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Up to three most recently used account + token pairs, newest first. The journal keeps identifiers only, so an
 * entry whose account or token is no longer in the wallet is dropped rather than shown as a dead tile.
 */
class GetRecentTokenTransfersUseCase(
    private val recentsStore: PaymentAccountRecentsStore,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<RecentTokenTransfer>> {
        return singleAccountListSupplier(userWalletId).map { accountList ->
            join(usage = recentsStore.getUsage(userWalletId), accountList = accountList)
        }
    }

    private fun join(usage: Map<AccountCurrencyId, Long>, accountList: AccountList): List<RecentTokenTransfer> {
        val accounts = accountList.accounts
            .filterIsInstance<Account.CryptoPortfolio>()
            .associateBy(Account.CryptoPortfolio::accountId)

        return usage.entries
            .sortedByDescending { it.value }
            .mapNotNull { (key, _) ->
                val (accountId, currencyId) = key
                val account = accounts[accountId] ?: return@mapNotNull null
                val currency = account.cryptoCurrencies.firstOrNull { it.id == currencyId }
                    ?: return@mapNotNull null

                RecentTokenTransfer(account = account, currency = currency)
            }
            .take(MAX_RECENTS)
    }

    private companion object {
        const val MAX_RECENTS = 3
    }
}