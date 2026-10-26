package com.tangem.data.walletconnect.network.ethereum

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.domain.walletconnect.usecase.method.WcTransactionTargetCheck.Status
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Checks an `eth_sendTransaction` / `eth_signTransaction` payload against the network it was requested on:
 * the chain the dApp built it for first, then — only while that matches — whether its recipient can run
 * the call data it carries.
 */
internal class WcEthTransactionTargetValidator @Inject constructor(
    private val walletManagersFacade: WalletManagersFacade,
) {

    fun validate(transaction: WcEthTransactionParams, userWalletId: UserWalletId, network: Network): Flow<Status> =
        flow {
            val chainMismatch = findChainMismatch(transaction, network)
            if (chainMismatch != null) {
                emit(chainMismatch)
                return@flow
            }

            if (!transaction.hasCallData) {
                emit(Status.Clear)
                return@flow
            }

            emit(Status.Checking)
            emit(checkRecipient(recipient = transaction.to, userWalletId = userWalletId, network = network))
        }

    private fun findChainMismatch(transaction: WcEthTransactionParams, network: Network): Status.ChainMismatch? {
        val declaredChainId = transaction.declaredChainId ?: return null
        val requestedChainId = network.toBlockchain().getChainId() ?: return null
        if (declaredChainId == requestedChainId) return null

        TangemLogger.withTag(WC_TAG).e(
            "Transaction declares chain ID $declaredChainId but was requested on ${network.name} " +
                "(chain ID $requestedChainId)",
        )
        return Status.ChainMismatch(declaredChainId = declaredChainId)
    }

    private suspend fun checkRecipient(recipient: String?, userWalletId: UserWalletId, network: Network): Status {
        if (recipient == null) return Status.Clear

        val hasCode = walletManagersFacade.hasContractCode(
            userWalletId = userWalletId,
            network = network,
            address = recipient,
        )
        TangemLogger.withTag(WC_TAG).i("Recipient $recipient on ${network.name} has contract code: $hasCode")

        // A failed lookup comes back as null and must not stand between the user and a transaction they asked for.
        return if (hasCode == false) Status.RecipientIsNotContract else Status.Clear
    }
}