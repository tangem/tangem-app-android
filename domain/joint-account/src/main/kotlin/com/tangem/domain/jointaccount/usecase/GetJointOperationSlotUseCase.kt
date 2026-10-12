package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSlot
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network

/**
 * The operation slot of [Account.Joint] in a [Network]. Only one operation may exist per (account, network) at a
 * time, so a busy slot stops the send flow before the user fills in anything. Data that is not fresh from the
 * backend is refetched first; when that fails the slot is [JointOperationSlot.UNKNOWN] — an empty cache must never
 * read as a free slot.
 */
class GetJointOperationSlotUseCase(
    private val supplier: SingleJointAccountListSupplier,
    private val fetcher: SingleJointAccountListFetcher,
    private val networkReader: JointNetworkReader,
) {

    suspend operator fun invoke(account: Account.Joint, network: Network): JointOperationSlot {
        val chainId = networkReader.getChainId(network = network).getOrNull() ?: return JointOperationSlot.UNKNOWN
        val jointAccount = findActual(account = account) ?: return JointOperationSlot.UNKNOWN

        return if (jointAccount.inFlightOperations[chainId] != null) {
            JointOperationSlot.BUSY
        } else {
            JointOperationSlot.FREE
        }
    }

    private suspend fun findActual(account: Account.Joint): JointAccount? {
        val cached = supplier.find(accountId = account.accountId)
        if (cached?.source == StatusSource.ACTUAL) return cached

        return fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = account.accountId.userWalletId))
            .fold(
                ifLeft = { null },
                ifRight = { supplier.find(accountId = account.accountId) },
            )
    }
}