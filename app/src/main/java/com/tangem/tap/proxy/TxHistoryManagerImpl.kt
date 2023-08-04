package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.blockchain.common.txhistory.TransactionHistoryState
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.TxHistoryManager
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.txhistory.ProxyTransactionHistoryItem
import com.tangem.lib.crypto.models.txhistory.ProxyTransactionHistoryState
import com.tangem.lib.crypto.models.txhistory.ProxyTransactionStatus

class TxHistoryManagerImpl(
    private val appStateHolder: AppStateHolder,
) : TxHistoryManager {

    override suspend fun checkTxHistoryState(networkId: String, derivationPath: String?): ProxyTransactionHistoryState {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        val state = walletManager.getTransactionHistoryState(address = walletManager.wallet.address)
        return state.mapToProxy()
    }

    override suspend fun getTxHistoryItems(
        networkId: String,
        derivationPath: String?,
        page: Int,
        pageSize: Int,
    ): List<ProxyTransactionHistoryItem> {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        val itemsResult = walletManager.getTransactionsHistory(
            address = walletManager.wallet.address,
            page = page,
            pageSize = pageSize,
        )

        return when (itemsResult) {
            is Result.Success -> itemsResult.data.items.map { historyItem -> historyItem.mapToProxy() }
            is Result.Failure -> error(itemsResult.error.message ?: itemsResult.error.customMessage)
        }
    }

    private fun getActualWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager {
        val blockchainNetwork = BlockchainNetwork(blockchain, derivationPath, emptyList())
        val walletManager = appStateHolder.walletState?.getWalletManager(blockchainNetwork)
        return requireNotNull(walletManager) { "no wallet manager found" }
    }

    private fun TransactionHistoryState.mapToProxy(): ProxyTransactionHistoryState {
        return when (this) {
            TransactionHistoryState.Success.Empty -> ProxyTransactionHistoryState.Success.Empty
            is TransactionHistoryState.Failed.FetchError -> ProxyTransactionHistoryState.Failed.FetchError(exception)
            TransactionHistoryState.NotImplemented -> ProxyTransactionHistoryState.NotImplemented
            is TransactionHistoryState.Success.HasTransactions ->
                ProxyTransactionHistoryState.Success.HasTransactions(txCount)
        }
    }

    private fun TransactionHistoryItem.mapToProxy() = ProxyTransactionHistoryItem(
        txHash = txHash,
        timestamp = timestamp,
        direction = when (val direction = direction) {
            is TransactionHistoryItem.TransactionDirection.Incoming ->
                ProxyTransactionHistoryItem.TransactionDirection.Incoming(direction.from)
            is TransactionHistoryItem.TransactionDirection.Outgoing ->
                ProxyTransactionHistoryItem.TransactionDirection.Outgoing(direction.to)
        },
        status = when (status) {
            TransactionStatus.Confirmed -> ProxyTransactionStatus.Confirmed
            TransactionStatus.Unconfirmed -> ProxyTransactionStatus.Unconfirmed
        },
        type = when (type) {
            TransactionHistoryItem.TransactionType.Transfer -> ProxyTransactionHistoryItem.TransactionType.Transfer
        },
        amount = ProxyAmount(
            currencySymbol = amount.currencySymbol,
            value = requireNotNull(amount.value) { "Amount value must not be null" },
            decimals = amount.decimals,
        ),
    )
}
