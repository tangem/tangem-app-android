package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.verifier.JointOperationVerifier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

/**
 * Co-signs the in-flight operation of a joint account in [Network]: the operation is taken from the
 * store, the [JointOperationVerifier] pipeline and the Blockaid gate both stand between it and the card,
 * and only then the signature is collected and sent.
 */
@Suppress("LongParameterList")
class SignJointOperationUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointOperationSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
    private val verifier: JointOperationVerifier,
    private val blockaidGate: JointOperationBlockaidGate,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val networkReader: JointNetworkReader,
) {

    suspend operator fun invoke(
        account: Account.Joint,
        network: Network,
        displayed: JointTransferIntent,
    ): Either<JointOperationSignError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        val jointAccount = findJointAccount(userWalletId = userWalletId, cryptoAccountId = account.accountId.value)
        val safeAddress = ensureNotNull(jointAccount.safeAddress) {
            JointOperationSignError.VerificationFailed(
                reason = JointOperationSignError.VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH,
            )
        }

        val chainId = networkReader.getChainId(network = network)
            .mapLeft(JointOperationSignError::Failed)
            .bind()

        val operation = ensureNotNull(jointAccount.inFlightOperations[chainId]) {
            JointOperationSignError.OperationNotFound
        }
        when (operation.status) {
            JointOperation.Status.AWAITING_SIGNATURES -> Unit
            JointOperation.Status.PENDING -> raise(JointOperationSignError.ThresholdReached)
            JointOperation.Status.UNKNOWN -> raise(
                JointOperationSignError.Failed(
                    cause = IllegalStateException("The operation status is unknown to this build — not signing"),
                ),
            )
        }

        val ownerAddress = ownerAddressResolver
            .resolve(userWalletId = userWalletId, ownerKeyIndex = account.ownerKeyIndex)
            .mapLeft(JointOperationSignError::Failed)
            .bind()
            ?: raise(
                JointOperationSignError.VerificationFailed(
                    reason = JointOperationSignError.VerificationFailed.Reason.OWNER_KEY_NOT_DERIVED,
                ),
            )

        verifier
            .verify(
                jointAccount = jointAccount,
                operation = operation,
                displayed = displayed,
                ownerAddress = ownerAddress,
            )
            .bind()

        checkBlockaid(
            network = network,
            operation = operation,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
        )

        val signature = signer
            .signOperation(
                userWalletId = userWalletId,
                ownerKeyIndex = account.ownerKeyIndex,
                safeTx = operation.safeTx,
                chainId = chainId,
                safeAddress = safeAddress,
            )
            .mapLeft(::toSignError)
            .bind()

        repository
            .signOperation(
                userWalletId = userWalletId,
                operationId = operation.operationId,
                safeTx = operation.safeTx,
                signature = signature,
            )
            .bind()

        // The signature is already accepted by the backend, so a failed refresh must not fail the signing —
        // the list catches up on the next fetch
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}
    }

    private suspend fun Raise<JointOperationSignError>.checkBlockaid(
        network: Network,
        operation: JointOperation,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
    ) {
        val verdict = blockaidGate.verify(
            network = network,
            safeTx = operation.safeTx,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
        )

        ensure(verdict != JointOperationBlockaidGate.Verdict.MALICIOUS) { JointOperationSignError.Malicious }
    }

    private suspend fun Raise<JointOperationSignError>.findJointAccount(
        userWalletId: UserWalletId,
        cryptoAccountId: String,
    ): JointAccount {
        val accounts = supplier(userWalletId = userWalletId).firstOrNull().orEmpty()
        val own = accounts.firstOrNull { it.cryptoAccountId.equals(cryptoAccountId, ignoreCase = true) }

        return ensureNotNull(own) { JointOperationSignError.OperationNotFound }
    }

    private fun toSignError(error: JointOperationSigningError): JointOperationSignError = when (error) {
        is JointOperationSigningError.UserCancelled -> JointOperationSignError.UserCancelled
        is JointOperationSigningError.OwnerKeyNotDerived -> JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.OWNER_KEY_NOT_DERIVED,
        )
        is JointOperationSigningError.Failed -> JointOperationSignError.Failed(cause = error.cause)
    }
}