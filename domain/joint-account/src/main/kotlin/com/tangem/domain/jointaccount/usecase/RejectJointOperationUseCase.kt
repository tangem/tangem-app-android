package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network

/**
 * Refuses the operation of a joint account in [Network]: the initiator cancels it, any other member declines it.
 * The backend tells the two apart by the caller, so no card tap is involved.
 */
class RejectJointOperationUseCase(
    private val repository: JointAccountRepository,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
    private val networkReader: JointNetworkReader,
) {

    suspend operator fun invoke(
        account: Account.Joint,
        network: Network,
        operationId: String,
    ): Either<JointOperationRejectError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        val jointAccount = ensureNotNull(supplier.find(accountId = account.accountId)) {
            JointOperationRejectError.OperationNotFound
        }
        val chainId = networkReader.getChainId(network = network)
            .mapLeft(JointOperationRejectError::Failed)
            .bind()
        val operation = refusableOperation(jointAccount = jointAccount, chainId = chainId, operationId = operationId)

        repository.rejectOperation(userWalletId = userWalletId, operationId = operation.operationId).bind()

        // The backend has already applied the refusal, so a failed refresh must not report it as failed — the
        // list catches up on the next fetch
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}
    }

    /** A replacement in the same slot is not what the user saw and refuses */
    private fun Raise<JointOperationRejectError>.refusableOperation(
        jointAccount: JointAccount,
        chainId: Long,
        operationId: String,
    ): JointOperation {
        val operation = ensureNotNull(
            value = jointAccount.inFlightOperations[chainId]?.takeIf { it.operationId == operationId },
        ) { JointOperationRejectError.OperationNotFound }

        return when (operation.status) {
            JointOperation.Status.AWAITING_SIGNATURES -> operation
            JointOperation.Status.PENDING -> raise(JointOperationRejectError.ThresholdReached)
            JointOperation.Status.UNKNOWN -> raise(
                JointOperationRejectError.Failed(
                    cause = IllegalStateException("The operation status is unknown to this build — not refusing"),
                ),
            )
        }
    }
}