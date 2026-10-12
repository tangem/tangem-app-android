package com.tangem.features.txhistory.joint.requests.model

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.joint.ownJointAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * A joint account holds one operation per network at a time, so there is at most one: awaiting signatures, or past
 * the threshold and on its way to the network until the chain reports it.
 */
internal class JointCosignRequestsDelegate @Inject constructor(
    private val accountListSupplier: SingleAccountListSupplier,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val networkReader: JointNetworkReader,
    private val transferResolver: JointOperationTransferResolver,
) {

    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<JointCosignRequest?> {
        if (currency.network.derivationPath !is Network.DerivationPath.Joint) return flowOf(null)

        return combine(
            flow = accountListSupplier.ownJointAccount(userWalletId = userWalletId, currency = currency),
            flow2 = jointAccountListSupplier(userWalletId = userWalletId),
        ) { account, accounts -> resolve(account = account, accounts = accounts, currency = currency) }
            .distinctUntilChanged()
    }

    private suspend fun resolve(
        account: Account.Joint?,
        accounts: List<JointAccount>,
        currency: CryptoCurrency,
    ): JointCosignRequest? {
        if (account == null) return null
        val jointAccount = accounts
            .firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
            ?: return null
        val safeAddress = jointAccount.safeAddress ?: return null
        val chainId = networkReader.getChainId(network = currency.network).getOrNull() ?: return null

        val operation = jointAccount.inFlightOperations[chainId]
            ?.takeIf { it.status != JointOperation.Status.UNKNOWN }
            ?: return null
        // The row belongs to one token: an operation moving another token of the same network stays out
        val transfer = transferResolver.resolve(
            safeTx = operation.safeTx,
            currencies = listOf(currency),
            network = currency.network,
            safeAddress = safeAddress,
        ) ?: return null

        return JointCosignRequest(operation = operation, transfer = transfer)
    }
}