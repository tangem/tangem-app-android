package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationRejectPayload
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network

/**
 * Refuses the operation of a joint account in [Network]: the initiator cancels it, any other member declines it.
 * The backend tells the two apart by the signer, so the app only signs the operation with the owner key and sends it.
 */
class RejectJointOperationUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointAccountSigner,
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

        val derivationIndex = DerivationIndex(value = account.ownerKeyIndex.value)
            .mapLeft(::invalidOwnerKeyIndex)
            .bind()
        val payload = JointOperationRejectPayload(operationId = operation.operationId, safeTx = operation.safeTx)

        val signResult = signer
            .sign(
                userWalletId = userWalletId,
                input = JointAccountSignInput(derivationIndex = derivationIndex) { payload },
            )
            .mapLeft(::toRejectError)
            .bind()

        repository
            .rejectOperation(userWalletId = userWalletId, payload = payload, signature = signResult.signature)
            .bind()

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

    private fun toRejectError(cause: Throwable): JointOperationRejectError = when (cause) {
        is TangemSdkError.UserCancelled -> JointOperationRejectError.UserCancelled
        else -> JointOperationRejectError.Failed(cause = cause)
    }

    private fun invalidOwnerKeyIndex(error: DerivationIndex.Error): JointOperationRejectError.Failed {
        return JointOperationRejectError.Failed(cause = IllegalStateException(error.toString()))
    }
}