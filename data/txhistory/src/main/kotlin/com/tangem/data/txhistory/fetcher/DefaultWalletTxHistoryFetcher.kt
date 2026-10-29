package com.tangem.data.txhistory.fetcher

import androidx.annotation.VisibleForTesting
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.defaultLaunchIn
import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.receiveTrigger
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

internal class DefaultWalletTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val walletId: UserWalletId,
    private val utils: TxHistoryFetcherUtils,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    expressTxHistoryFetcher: DefaultExpressTxHistoryFetcher.Factory,
    private val jointOperationTxHistoryFetcher: DefaultJointOperationTxHistoryFetcher.Factory,
) : WalletTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val expressFetcher: ExpressTxHistoryFetcher = expressTxHistoryFetcher.create(walletId)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var jointOperationFetcher: JointOperationTxHistoryFetcher? = null

    init {
        defaultLaunchIn(buildFlow())
    }

    override fun invoke(params: TxHistoryFetchTrigger) {
        sendTrigger(params)
    }

    override fun close() {
        cancelScope()
        expressFetcher.close()
        jointOperationFetcher?.close()
    }

    private fun routeToWalletFetchers(trigger: TxHistoryFetchTrigger) {
        if (trigger is TxHistoryExpressTrigger) expressFetcher.invoke(trigger)
        if (trigger is TxHistoryJointOperationTrigger) jointOperationFetcher?.invoke(trigger)
    }

    // Kept until per-account on-chain history is wired: only the wallet-level part (express, joint operations) is live for now.
    @Suppress("UnusedPrivateMember")
    private fun buildFlow(): Flow<Unit> = channelFlow {
        val accountListFlow = singleAccountListSupplier(walletId)
            .stateIn(this)

        fun accountList(): AccountList = accountListFlow.value
        accountList().accounts
            .createForNewAccounts()

        accountListFlow
            .map { accountList -> accountList.accounts }
            .distinctUntilChanged()
            .onEach { accounts -> accounts.createForNewAccounts() }
            .closeForRemovedAccounts()
            .launchIn(this)

        receiveTrigger()
            .onEach { trigger -> routeToWalletFetchers(trigger) }
            .collect {}
    }

    private fun List<Account>.createForNewAccounts() = forEach { account ->
        if (account is Account.Joint && jointOperationFetcher == null) {
            jointOperationFetcher = jointOperationTxHistoryFetcher.create(walletId)
        }
    }

    private fun Flow<List<Account>>.closeForRemovedAccounts() = runningReduce { previousIds, newIds ->
        val isJointExist = newIds.any { account -> account is Account.Joint }
        if (!isJointExist) {
            jointOperationFetcher?.close()
            jointOperationFetcher = null
        }
        newIds
    }

    @AssistedFactory
    internal interface Factory {
        fun create(walletId: UserWalletId): DefaultWalletTxHistoryFetcher
    }
}