package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import java.math.BigInteger

/**
 * Creates a send operation of a joint account: the slot gate on a fresh account read, the on-chain
 * composition check, the payload-form check, then build → sign → create.
 */
@Suppress("LongParameterList")
class CreateJointOperationUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointOperationSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
    private val gasBuilder: SafeTxGasBuilder,
    private val compositionChecker: SafeCompositionChecker,
    private val networkReader: JointNetworkReader,
    private val payloadCodec: JointTransferPayloadCodec,
) {

    suspend operator fun invoke(
        account: Account.Joint,
        network: Network,
        intent: JointTransferIntent,
    ): Either<JointOperationCreateError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        // The slot gate reads the backend, not the cache: another member may have taken the slot
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId))
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        val jointAccount = ensureNotNull(supplier.find(accountId = account.accountId)) {
            JointOperationCreateError.AccountNotReady
        }
        ensure(jointAccount.status == JointAccount.Status.ACTIVE) { JointOperationCreateError.AccountNotReady }
        val safeAddress = ensureNotNull(jointAccount.safeAddress) { JointOperationCreateError.AccountNotReady }

        val chainId = networkReader.getChainId(network = network)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        ensure(jointAccount.inFlightOperations[chainId] == null) { JointOperationCreateError.SlotBusy }

        val call = createInnerCall(intent = intent, safeAddress = safeAddress).bind()

        val created = checkBuildSignAndCreate(
            account = account,
            jointAccount = jointAccount,
            network = network,
            call = call,
            chainId = chainId,
            safeAddress = safeAddress,
        )

        created.getOrElse { error ->
            // The network moved under the first attempt — re-check, rebuild from fresh state and ask for a new signature
            if (error is JointOperationCreateError.NetworkStateMoved) {
                checkBuildSignAndCreate(
                    account = account,
                    jointAccount = jointAccount,
                    network = network,
                    call = call,
                    chainId = chainId,
                    safeAddress = safeAddress,
                ).bind()
            } else {
                raise(error)
            }
        }

        // The operation exists on the backend, so a failed refresh must not fail the creation — the list
        // catches up on the next fetch; only success is never reported before this read completes
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}
    }

    /** One signing attempt — the composition is re-checked every time a signature is about to be requested */
    @Suppress("LongParameterList")
    private suspend fun checkBuildSignAndCreate(
        account: Account.Joint,
        jointAccount: JointAccount,
        network: Network,
        call: SafeInnerCall,
        chainId: Long,
        safeAddress: String,
    ): Either<JointOperationCreateError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        compositionChecker
            .ensureMatches(
                userWalletId = userWalletId,
                network = network,
                safeAddress = safeAddress,
                owners = jointAccount.members.map(JointAccount.Member::address),
                threshold = jointAccount.threshold,
            )
            .bind()

        val safeTx = gasBuilder
            .build(userWalletId = userWalletId, network = network, call = call)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        val signature = signer
            .signOperation(
                userWalletId = userWalletId,
                ownerKeyIndex = account.ownerKeyIndex,
                safeTx = safeTx,
                chainId = chainId,
                safeAddress = safeAddress,
            )
            .mapLeft(::toCreateError)
            .bind()

        repository
            .createOperation(
                userWalletId = userWalletId,
                safeAddress = safeAddress,
                chainId = chainId,
                safeTx = safeTx,
                signature = signature,
            )
            .bind()
    }

    /** Only the two known transfer shapes exist, and the Safe never calls itself */
    private fun createInnerCall(
        intent: JointTransferIntent,
        safeAddress: String,
    ): Either<JointOperationCreateError, SafeInnerCall> {
        if (intent.amountWei <= BigInteger.ZERO) return JointOperationCreateError.InvalidPayload.left()
        if (intent.amountWei.bitLength() > UINT256_BITS) return JointOperationCreateError.InvalidPayload.left()
        if (!ADDRESS_FORMAT.matches(intent.recipient)) return JointOperationCreateError.InvalidPayload.left()

        if (intent is JointTransferIntent.Erc20 && !ADDRESS_FORMAT.matches(intent.tokenContract)) {
            return JointOperationCreateError.InvalidPayload.left()
        }

        val call = payloadCodec.encode(intent) ?: return JointOperationCreateError.InvalidPayload.left()

        return if (call.to.equals(safeAddress, ignoreCase = true)) {
            JointOperationCreateError.InvalidPayload.left()
        } else {
            call.right()
        }
    }

    private fun toCreateError(error: JointOperationSigningError): JointOperationCreateError = when (error) {
        is JointOperationSigningError.UserCancelled -> JointOperationCreateError.UserCancelled
        is JointOperationSigningError.OwnerKeyNotDerived -> JointOperationCreateError.Failed(
            cause = IllegalStateException("The owner key of the account is not derived"),
        )
        is JointOperationSigningError.Failed -> JointOperationCreateError.Failed(cause = error.cause)
    }

    private companion object {
        const val UINT256_BITS = 256
        val ADDRESS_FORMAT = Regex(pattern = "^0x[0-9a-fA-F]{40}$")
    }
}