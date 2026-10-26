package com.tangem.data.txhistory.list

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Finished joint operations that moved [CryptoCurrency] out of its joint account, as history rows not yet matched to
 * the chain. An operation moving another token of the same network, or doing anything but a transfer, stays out.
 */
internal class FinishedJointTransfers @Inject constructor(
    private val accountListSupplier: SingleAccountListSupplier,
    private val networkReader: JointNetworkReader,
    private val transferResolver: JointOperationTransferResolver,
    private val repository: JointOperationHistoryRepository,
) {

    /**
     * @param fromFinishedAtMillis no skew is applied: the backend marks an operation finished only after its receipt,
     * so it never finishes earlier than its block
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fromFinishedAtMillis: Long,
    ): Flow<List<JointOperationTx>> {
        if (!currency.isJoint) return flowOf(emptyList())

        return accountListSupplier(userWalletId)
            .map { accountList ->
                accountList.accounts
                    .filterIsInstance<Account.Joint>()
                    .firstOrNull { account -> account.cryptoCurrencies.any { it.network == currency.network } }
                    ?.accountId
            }
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                val chainId = networkReader.getChainId(currency.network).getOrNull()
                if (accountId == null || chainId == null) return@flatMapLatest flowOf(emptyList())

                repository.getFinishedOperations(
                    accountId = accountId,
                    chainId = chainId,
                    fromFinishedAtMillis = fromFinishedAtMillis,
                ).map { operations ->
                    operations.mapNotNull { operation ->
                        val transfer = transferResolver.resolve(
                            safeTx = operation.safeTx,
                            currencies = listOf(currency),
                            network = currency.network,
                            safeAddress = operation.safeAddress,
                        ) ?: return@mapNotNull null

                        JointOperationTx(operation = operation, transfer = transfer, txInfo = null)
                    }
                }
            }
    }
}