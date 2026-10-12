package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import kotlinx.coroutines.flow.firstOrNull

/**
 * Whether [Account.Joint] already holds a non-terminal operation in a [Network]. Only one operation may exist per
 * (account, network) at a time, so a busy slot stops the send flow before the user fills in anything.
 */
class IsJointOperationSlotBusyUseCase(
    private val supplier: SingleJointAccountListSupplier,
    private val networkReader: JointNetworkReader,
) {

    suspend operator fun invoke(account: Account.Joint, network: Network): Boolean {
        val chainId = networkReader.getChainId(network = network).getOrNull() ?: return false
        val jointAccount = supplier(userWalletId = account.accountId.userWalletId)
            .firstOrNull()
            ?.firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
            ?: return false

        return jointAccount.inFlightOperations[chainId] != null
    }
}