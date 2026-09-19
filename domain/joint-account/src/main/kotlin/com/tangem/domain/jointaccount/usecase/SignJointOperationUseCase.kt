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
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.jointaccount.verifier.JointOperationVerifier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Co-signs the operation of a joint account in [Network]: the operation is taken from the store by the id the
 * caller showed, the [JointOperationVerifier] pipeline and the Blockaid gate both stand between it and the card,
 * and only then the signature is collected and sent. An operation the slot no longer holds under that id — it
 * finished, was cancelled, or was replaced by another one — is [JointOperationSignError.OperationNotFound].
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
    private val compositionChecker: SafeCompositionChecker,
    private val refundReceiverProvider: JointRefundReceiverProvider,
) {

    suspend operator fun invoke(
        account: Account.Joint,
        network: Network,
        operationId: String,
        displayed: JointTransferIntent,
    ): Either<JointOperationSignError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        val jointAccount = ensureNotNull(supplier.find(accountId = account.accountId)) {
            JointOperationSignError.OperationNotFound
        }
        val safeAddress = ensureNotNull(jointAccount.safeAddress) {
            JointOperationSignError.VerificationFailed(
                reason = JointOperationSignError.VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH,
            )
        }

        val chainId = networkReader.getChainId(network = network)
            .mapLeft(JointOperationSignError::Failed)
            .bind()

        val operation = signableOperation(
            jointAccount = jointAccount,
            chainId = chainId,
            operationId = operationId,
        )
        val ownerAddress = ownOwnerAddress(userWalletId = userWalletId, account = account)

        verifier
            .verify(
                jointAccount = jointAccount,
                operation = operation,
                displayed = displayed,
                ownerAddress = ownerAddress,
                refundBounds = refundBounds(userWalletId = userWalletId, network = network),
            )
            .bind()

        val deployment = checkComposition(
            userWalletId = userWalletId,
            network = network,
            safeAddress = safeAddress,
            jointAccount = jointAccount,
        )

        checkBlockaid(
            network = network,
            operation = operation,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
            deployment = deployment,
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

    /**
     * The slot holds one operation at a time, and the signature is bound to the one the screen showed: a
     * replacement carrying the same payload must not be signed instead of it. An unknown status blocks too —
     * this build cannot tell whether such an operation still needs a signature.
     */
    private fun Raise<JointOperationSignError>.signableOperation(
        jointAccount: JointAccount,
        chainId: Long,
        operationId: String,
    ): JointOperation {
        val operation = ensureNotNull(
            value = jointAccount.inFlightOperations[chainId]?.takeIf { it.operationId == operationId },
        ) { JointOperationSignError.OperationNotFound }

        return when (operation.status) {
            JointOperation.Status.AWAITING_SIGNATURES -> operation
            JointOperation.Status.PENDING -> raise(JointOperationSignError.ThresholdReached)
            JointOperation.Status.UNKNOWN -> raise(
                JointOperationSignError.Failed(
                    cause = IllegalStateException("The operation status is unknown to this build — not signing"),
                ),
            )
        }
    }

    private suspend fun Raise<JointOperationSignError>.ownOwnerAddress(
        userWalletId: UserWalletId,
        account: Account.Joint,
    ): String {
        val address = ownerAddressResolver
            .resolve(userWalletId = userWalletId, ownerKeyIndex = account.ownerKeyIndex)
            .mapLeft(JointOperationSignError::Failed)
            .bind()

        return ensureNotNull(address) {
            JointOperationSignError.VerificationFailed(
                reason = JointOperationSignError.VerificationFailed.Reason.OWNER_KEY_NOT_DERIVED,
            )
        }
    }

    /**
     * The composition the app knows is checked against the deployed Safe, the same way creation does it: a backend
     * reporting different owners or a different threshold must not get a signature for that composition.
     */
    private suspend fun Raise<JointOperationSignError>.checkComposition(
        userWalletId: UserWalletId,
        network: Network,
        safeAddress: String,
        jointAccount: JointAccount,
    ): SafeDeployment {
        return compositionChecker
            .ensureMatches(
                userWalletId = userWalletId,
                network = network,
                safeAddress = safeAddress,
                owners = jointAccount.members.map(JointAccount.Member::address),
                threshold = jointAccount.threshold,
            )
            .mapLeft(::toSignError)
            .bind()
    }

    private suspend fun Raise<JointOperationSignError>.checkBlockaid(
        network: Network,
        operation: JointOperation,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
        deployment: SafeDeployment,
    ) {
        val verdict = blockaidGate.verify(
            network = network,
            safeTx = operation.safeTx,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
        )

        when (verdict) {
            JointOperationBlockaidGate.Verdict.SAFE,
            JointOperationBlockaidGate.Verdict.WARNING,
            -> Unit
            JointOperationBlockaidGate.Verdict.MALICIOUS -> raise(JointOperationSignError.Malicious)
            // No answer is not a pass: a co-signer cannot read the payload, so an unverified one is not signed
            JointOperationBlockaidGate.Verdict.UNAVAILABLE -> raise(
                JointOperationSignError.Failed(cause = IllegalStateException("Blockaid is unavailable")),
            )
            // Blockaid judges the signature by reading the Safe contract; before the first operation deploys it
            // there is nothing to read and no verdict can come. The CREATE2 check above already proved the payload
            // targets our composition, so only a deployed Safe left without a verdict stays unsigned.
            JointOperationBlockaidGate.Verdict.UNKNOWN -> ensure(deployment == SafeDeployment.NOT_DEPLOYED) {
                JointOperationSignError.Failed(cause = IllegalStateException("Blockaid returned no verdict"))
            }
        }
    }

    private suspend fun Raise<JointOperationSignError>.refundBounds(
        userWalletId: UserWalletId,
        network: Network,
    ): JointOperationVerifier.RefundBounds {
        val refundReceiver = Either
            .catch { refundReceiverProvider.refundReceiver() }
            .mapLeft(JointOperationSignError::Failed)
            .bind()

        val gasPrice = networkReader
            .getGasPrice(userWalletId = userWalletId, network = network)
            .mapLeft(JointOperationSignError::Failed)
            .bind()

        return JointOperationVerifier.RefundBounds(refundReceiver = refundReceiver, networkGasPrice = gasPrice)
    }

    private fun toSignError(error: JointOperationCreateError): JointOperationSignError = when (error) {
        is JointOperationCreateError.CompositionMismatch -> JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.COMPOSITION_MISMATCH,
        )
        is JointOperationCreateError.UserCancelled -> JointOperationSignError.UserCancelled
        is JointOperationCreateError.Failed -> JointOperationSignError.Failed(cause = error.cause)
        else -> JointOperationSignError.Failed(
            cause = IllegalStateException("Unexpected composition check outcome: $error"),
        )
    }

    private fun toSignError(error: JointOperationSigningError): JointOperationSignError = when (error) {
        is JointOperationSigningError.UserCancelled -> JointOperationSignError.UserCancelled
        is JointOperationSigningError.OwnerKeyNotDerived -> JointOperationSignError.VerificationFailed(
            reason = JointOperationSignError.VerificationFailed.Reason.OWNER_KEY_NOT_DERIVED,
        )
        is JointOperationSigningError.Failed -> JointOperationSignError.Failed(cause = error.cause)
    }
}