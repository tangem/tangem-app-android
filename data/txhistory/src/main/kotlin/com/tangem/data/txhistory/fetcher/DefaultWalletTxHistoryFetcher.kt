package com.tangem.data.txhistory.fetcher

import androidx.annotation.VisibleForTesting
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTrigger
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyOperations.getAccountCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.AccountTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.txhistory.fetcher.WalletTxHistoryFetcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

internal class DefaultWalletTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val walletId: UserWalletId,
    private val utils: TxHistoryFetcherUtils,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val accountTxHistoryFetcher: DefaultAccountTxHistoryFetcher.Factory,
) : WalletTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val fetchers = ConcurrentHashMap<AccountId, AccountTxHistoryFetcher>()

    init {
        defaultLaunchIn(buildFlow())
    }

    override suspend fun invoke(params: TxHistoryFetchTrigger) {
        sendTrigger(params)
    }

    override fun close() {
        cancelScope()
        fetchers.forEach { (_, fetcher) -> fetcher.close() }
        fetchers.clear()
    }

    private fun buildFlow(): Flow<Unit> = channelFlow {
        val accountListFlow = singleAccountListSupplier(walletId)
            .stateIn(this)

        fun accountList(): AccountList = accountListFlow.value
        accountList().accounts
            .mapTo(mutableSetOf()) { it.accountId }
            .createForNewAccounts()

        accountListFlow
            .map { accountList -> accountList.accounts.mapTo(mutableSetOf()) { account -> account.accountId } }
            .distinctUntilChanged()
            .createForNewAccounts()
            .closeForRemovedAccounts()
            .launchIn(this)

        receiveTrigger()
            .onEach { trigger ->
                when (trigger) {
                    is TxHistoryFetchTrigger.TokenDetailsOpen -> accountList()
                        .findFetcher(trigger.currency)?.invoke(trigger)
                    is TxHistoryFetchTrigger.TokenDetailsPTR -> accountList()
                        .findFetcher(trigger.currency)?.invoke(trigger)
                }
            }
            .collect {}
    }

    private fun Flow<Set<AccountId>>.createForNewAccounts() = onEach { ids -> ids.createForNewAccounts() }

    private fun Set<AccountId>.createForNewAccounts() = this.forEach { id -> getOrPutFetcher(id) }

    private fun Flow<Set<AccountId>>.closeForRemovedAccounts() = runningReduce { previousIds, newIds ->
        val removedWallets = previousIds.subtract(newIds)
        removedWallets.forEach { walletId -> fetchers.remove(walletId)?.close() }
        newIds
    }

    private fun AccountList.findFetcher(currency: CryptoCurrency): AccountTxHistoryFetcher? = this
        .getAccountCryptoCurrency(currency)
        .getOrNull()
        ?.account
        ?.let { account -> fetchers[account.accountId] }

    private fun getOrPutFetcher(id: AccountId): AccountTxHistoryFetcher {
        return fetchers.computeIfAbsent(id) { createFetcher(id) }
    }

    private fun createFetcher(id: AccountId): AccountTxHistoryFetcher {
        return accountTxHistoryFetcher.create(id)
    }

    @AssistedFactory
    internal interface Factory {
        fun create(walletId: UserWalletId): DefaultWalletTxHistoryFetcher
    }
}