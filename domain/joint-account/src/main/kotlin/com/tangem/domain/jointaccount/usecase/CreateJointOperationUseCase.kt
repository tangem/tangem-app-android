package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.PreparedJointOperation
import com.tangem.domain.jointaccount.model.SafeGasToken
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
 * composition check, the payload-form check, then build → Blockaid in [prepare] and sign → create in [submit].
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
    private val blockaidGate: JointOperationBlockaidGate,
    private val ownerAddressResolver: JointOwnerAddressResolver,
) {

    suspend fun prepare(
        account: Account.Joint,
        network: Network,
        intent: JointTransferIntent,
        gasToken: SafeGasToken,
    ): Either<JointOperationCreateError, PreparedJointOperation.Creation> = either {
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

        val ownerAddress = ownerAddressResolver
            .resolve(userWalletId = userWalletId, ownerKeyIndex = account.ownerKeyIndex)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()
            ?: raise(JointOperationCreateError.Failed(cause = IllegalStateException("The owner key is not derived")))

        checkBuildAndVerify(
            account = account,
            network = network,
            jointAccount = jointAccount,
            call = call,
            gasToken = gasToken,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
        ).bind()
    }

    suspend fun submit(prepared: PreparedJointOperation.Creation): Either<JointOperationCreateError, SubmitResult> =
        either {
            ensure(prepared.verdict != JointOperationBlockaidGate.Verdict.Malicious) {
                JointOperationCreateError.Malicious
            }

            signAndCreate(prepared = prepared).fold(
                ifLeft = { error -> rebuildAfterNetworkMoved(prepared = prepared, error = error).bind() },
                ifRight = {
                    // The operation exists on the backend, so a failed refresh must not fail the creation — the list
                    // catches up on the next fetch; only success is never reported before this read completes
                    val userWalletId = prepared.account.accountId.userWalletId
                    fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}
                    SubmitResult.Created
                },
            )
        }

    private suspend fun rebuildAfterNetworkMoved(
        prepared: PreparedJointOperation.Creation,
        error: JointOperationCreateError,
    ): Either<JointOperationCreateError, SubmitResult.Rebuilt> = either {
        ensure(error is JointOperationCreateError.NetworkStateMoved && !prepared.isRebuilt) { error }

        val rebuilt = with(prepared) {
            checkBuildAndVerify(
                account = account,
                network = network,
                jointAccount = jointAccount,
                call = call,
                gasToken = gasToken,
                chainId = chainId,
                safeAddress = safeAddress,
                ownerAddress = ownerAddress,
            )
        }.bind().copy(isRebuilt = true)

        SubmitResult.Rebuilt(prepared = rebuilt)
    }

    private suspend fun checkBuildAndVerify(
        account: Account.Joint,
        network: Network,
        jointAccount: JointAccount,
        call: SafeInnerCall,
        gasToken: SafeGasToken,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
    ): Either<JointOperationCreateError, PreparedJointOperation.Creation> = either {
        val userWalletId = account.accountId.userWalletId

        val deployment = compositionChecker
            .ensureMatches(
                userWalletId = userWalletId,
                network = network,
                safeAddress = safeAddress,
                owners = jointAccount.members.map(JointAccount.Member::address),
                threshold = jointAccount.threshold,
            )
            .bind()

        val safeTx = gasBuilder
            .build(userWalletId = userWalletId, network = network, call = call, gasToken = gasToken)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        val verdict = blockaidGate.verify(
            network = network,
            safeTx = safeTx,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
            deployment = deployment,
        )

        PreparedJointOperation.Creation(
            account = account,
            network = network,
            safeTx = safeTx,
            verdict = verdict,
            jointAccount = jointAccount,
            call = call,
            gasToken = gasToken,
            chainId = chainId,
            safeAddress = safeAddress,
            ownerAddress = ownerAddress,
        )
    }

    private suspend fun signAndCreate(
        prepared: PreparedJointOperation.Creation,
    ): Either<JointOperationCreateError, Unit> = either {
        val userWalletId = prepared.account.accountId.userWalletId

        val signature = signer
            .signOperation(
                userWalletId = userWalletId,
                ownerKeyIndex = prepared.account.ownerKeyIndex,
                safeTx = prepared.safeTx,
                chainId = prepared.chainId,
                safeAddress = prepared.safeAddress,
            )
            .mapLeft(::toCreateError)
            .bind()

        repository
            .createOperation(
                userWalletId = userWalletId,
                safeAddress = prepared.safeAddress,
                chainId = prepared.chainId,
                safeTx = prepared.safeTx,
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

    sealed interface SubmitResult {

        data object Created : SubmitResult

        data class Rebuilt(val prepared: PreparedJointOperation.Creation) : SubmitResult
    }

    private companion object {
        const val UINT256_BITS = 256
        val ADDRESS_FORMAT = Regex(pattern = "^0x[0-9a-fA-F]{40}$")
    }
}