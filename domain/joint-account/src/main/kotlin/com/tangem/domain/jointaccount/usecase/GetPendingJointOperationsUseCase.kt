package com.tangem.domain.jointaccount.usecase

import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A member who declined is not told about the operation more.
 * [accounts] are the wallet's joint accounts as the caller already holds them, the way the rest of this domain
 * takes an [Account.Joint] rather than looking the account list up itself.
 */
class GetPendingJointOperationsUseCase(
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val transferResolver: JointOperationTransferResolver,
    private val signersResolver: JointOperationSignersResolver,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val networkReader: JointNetworkReader,
) {

    operator fun invoke(userWalletId: UserWalletId, accounts: List<Account.Joint>): Flow<List<JointPendingOperation>> {
        return jointAccountListSupplier(userWalletId = userWalletId)
            .map { jointAccounts ->
                resolve(userWalletId = userWalletId, jointAccounts = jointAccounts, accounts = accounts)
            }
            .distinctUntilChanged()
    }

    private suspend fun resolve(
        userWalletId: UserWalletId,
        jointAccounts: List<JointAccount>,
        accounts: List<Account.Joint>,
    ): List<JointPendingOperation> {
        if (jointAccounts.isEmpty() || accounts.isEmpty()) return emptyList()

        return jointAccounts.flatMap { jointAccount ->
            val account = accounts
                .firstOrNull { it.accountId.value.equals(jointAccount.cryptoAccountId, ignoreCase = true) }
                ?: return@flatMap emptyList()

            val ownerAddress = ownerAddressResolver
                .resolve(userWalletId = userWalletId, ownerKeyIndex = account.ownerKeyIndex)
                .getOrNull()

            jointAccount.inFlightOperations.values
                .filter { it.status == JointOperation.Status.AWAITING_SIGNATURES }
                .mapNotNull { operation ->
                    resolveOperation(
                        jointAccount = jointAccount,
                        account = account,
                        operation = operation,
                        ownerAddress = ownerAddress,
                    )
                }
        }
    }

    private suspend fun resolveOperation(
        jointAccount: JointAccount,
        account: Account.Joint,
        operation: JointOperation,
        ownerAddress: String?,
    ): JointPendingOperation? {
        val safeAddress = jointAccount.safeAddress ?: return null
        val network = networkOf(account = account, chainId = operation.chainId) ?: return null
        val transfer = transferResolver.resolve(
            safeTx = operation.safeTx,
            currencies = account.cryptoCurrencies,
            network = network,
            safeAddress = safeAddress,
        ) ?: return null

        val signers = signersResolver.signers(jointAccount = jointAccount, operation = operation)
        val ownAnswer = ownerAddress?.let { own ->
            signers.firstOrNull { it.member.address.equals(own, ignoreCase = true) }?.answer
        }
        if (ownAnswer == Answer.DECLINED) return null

        return JointPendingOperation(
            account = account,
            operationId = operation.operationId,
            transfer = transfer,
            initiatorName = jointAccount.members
                .firstOrNull { it.address.equals(operation.initiator, ignoreCase = true) }
                ?.name,
            initiatorAddress = operation.initiator,
            isOwn = ownAnswer == Answer.CREATED,
            ownAnswer = ownAnswer,
            collected = signers.count { it.answer == Answer.CREATED || it.answer == Answer.SIGNED },
            threshold = jointAccount.threshold,
        )
    }

    private suspend fun networkOf(account: Account.Joint, chainId: Long): Network? {
        return account.cryptoCurrencies
            .map { it.network }
            .distinct()
            .firstOrNull { networkReader.getChainId(network = it).getOrNull() == chainId }
    }
}