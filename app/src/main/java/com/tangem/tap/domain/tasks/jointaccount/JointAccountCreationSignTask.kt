package com.tangem.tap.domain.tasks.jointaccount

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationSignInput
import com.tangem.domain.jointaccount.model.JointAccountCreationSignResult
import com.tangem.domain.jointaccount.signing.CanonicalJson
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Signs the joint account creation payload in a single NFC session: derives the owner key at
 * `m/44'/60'/888888'/0/{index}` for [JointAccountCreationSignInput.derivationIndex], computes its EVM address,
 * builds the payload around the address, and signs its EIP-191 hash over the RFC 8785 canonical form — all without
 * a second tap. `DeriveWalletPublicKeyTask` writes the derived key into the live session environment, so the
 * subsequent [SignHashCommand] resolves the derivation path within the same session.
 *
 * The task does not validate the index against the backend: the caller must take it from a freshly synced
 * `wallet.totalJointAccounts` right before starting the session (see [JointAccountCreationSignInput]). A stale
 * index is detected by the backend as a 409 on `POST /joint-accounts`.
 *
 * The derived key is returned (keyed by the seed wallet public key) so the caller can persist it via
 * `DerivationsRepository.storeDerivedKeys` — the session's in-memory copy dies with the session.
 *
 * Run via the generic `TangemSdkManager.runTaskAsync` with `UserWalletIdPreflightReadFilter` of the wallet the

 * not by a specific card id.
 */
class JointAccountCreationSignTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val input: JointAccountCreationSignInput,
) : CardSessionRunnable<JointAccountCreationSignResult> {

    override fun run(session: CardSession, callback: CompletionCallback<JointAccountCreationSignResult>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<JointAccountCreationSignResult> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?: return CompletionResult.Failure(TangemSdkError.WalletNotFound())
        val seedPublicKey = wallet.publicKey
            ?: return CompletionResult.Failure(TangemSdkError.WalletNotFound())

        val derivationPath = jointAccountOwnerDerivationPath(index = input.derivationIndex)
        val extendedPublicKey = when (val result = derive(session, seedPublicKey, derivationPath)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<ExtendedPublicKey> -> result.data
        }
        val ownerAddress = generateEvmAddress(extendedPublicKey)

        val payload = JointAccountCreationPayload(
            config = input.config,
            creator = JointAccountCreationPayload.Creator(
                walletId = input.walletId,
                name = input.creatorName,
                address = ownerAddress,
                derivation = input.derivationIndex,
            ),
        )
        val canonicalPayload = CanonicalJson.canonicalize(payload.toCanonicalMap())
        val hash = hashPersonalMessage(canonicalPayload)

        val signResult = sign(
            session = session,
            hash = hash,
            seedPublicKey = seedPublicKey,
            derivationPath = derivationPath,
        )
        val signResponse = when (signResult) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(signResult.error)
            is CompletionResult.Success<SignHashResponse> -> signResult.data
        }

        return CompletionResult.Success(
            data = JointAccountCreationSignResult(
                ownerAddress = ownerAddress,
                canonicalPayload = canonicalPayload,
                signature = toRsvHex(signResponse.signature, hash, extendedPublicKey),
                derivedKeys = mapOf(
                    seedPublicKey.toMapKey() to ExtendedPublicKeysMap(mapOf(derivationPath to extendedPublicKey)),
                ),
            ),
        )
    }

    private suspend fun derive(
        session: CardSession,
        seedPublicKey: ByteArray,
        derivationPath: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        DeriveWalletPublicKeyTask(walletPublicKey = seedPublicKey, derivationPath = derivationPath)
            .run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private suspend fun sign(
        session: CardSession,
        hash: ByteArray,
        seedPublicKey: ByteArray,
        derivationPath: DerivationPath,
    ): CompletionResult<SignHashResponse> {
        val deferred = CompletableDeferred<CompletionResult<SignHashResponse>>()
        SignHashCommand(hash = hash, walletPublicKey = seedPublicKey, derivationPath = derivationPath)
            .run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private fun generateEvmAddress(extendedPublicKey: ExtendedPublicKey): String {
        return Blockchain.Ethereum.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            rawPath = null,
            cachedIndex = null,
        ).address
    }

    private fun hashPersonalMessage(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        return (prefix + message).toKeccak()
    }

    private fun toRsvHex(signature: ByteArray, hash: ByteArray, extendedPublicKey: ExtendedPublicKey): String {
        val rsv = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = extendedPublicKey.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM()

        return "0x" + rsv.toHexString().lowercase()
    }

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope, input: JointAccountCreationSignInput): JointAccountCreationSignTask
    }
}