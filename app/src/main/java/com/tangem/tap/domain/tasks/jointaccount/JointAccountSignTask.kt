package com.tangem.tap.domain.tasks.jointaccount

import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
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
 * Signs a joint account payload in a single NFC session: derives the owner key at
 * `m/44'/60'/888888'/0/{index}` for [JointAccountSignInput.derivationIndex], computes its EVM address,
 * builds the payload around the address via [JointAccountSignInput.makePayload], and signs the payload's
 * EIP-191 hash over the RFC 8785 canonical form — all without a second tap. `DeriveWalletPublicKeyTask` writes
 * the derived key into the live session environment, so the subsequent [SignHashCommand] resolves the derivation
 * path within the same session.
 *
 * One task serves every signed joint account flow — creation, joining and activation differ only in the payload
 * and in where the index comes from (see [JointAccountSignInput]). The task does not validate the index against
 * the backend: a stale index is detected by the backend as a 409 after the tap is already spent.
 *
 * The derived key is returned (keyed by the seed wallet public key) so the caller can persist it via
 * `DerivationsRepository.storeDerivedKeys` — the session's in-memory copy dies with the session.
 *
 * Run via the generic `TangemSdkManager.runTaskAsync` with `UserWalletIdPreflightReadFilter` of the wallet the
 * payload belongs to: any backup card of that wallet signs equally, so the session is filtered by wallet,
 * not by a specific card id.
 */
class JointAccountSignTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val input: JointAccountSignInput,
) : CardSessionRunnable<JointAccountSignResult> {

    override fun run(session: CardSession, callback: CompletionCallback<JointAccountSignResult>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<JointAccountSignResult> {
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
        val ownerAddress = JointAccountSigning.evmAddress(ownerKey = extendedPublicKey)

        val payload = input.makePayload(ownerAddress)
        val canonicalPayload = CanonicalJson.canonicalize(payload.toCanonicalMap())
        val digest = JointAccountSigning.eip191Digest(canonicalPayload = canonicalPayload)

        val signResult = sign(
            session = session,
            hash = digest,
            seedPublicKey = seedPublicKey,
            derivationPath = derivationPath,
        )
        val signResponse = when (signResult) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(signResult.error)
            is CompletionResult.Success<SignHashResponse> -> signResult.data
        }

        return CompletionResult.Success(
            data = JointAccountSignResult(
                ownerAddress = ownerAddress,
                canonicalPayload = canonicalPayload,
                signature = JointAccountSigning.toRsvHex(
                    signature = signResponse.signature,
                    digest = digest,
                    ownerKey = extendedPublicKey,
                ),
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

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope, input: JointAccountSignInput): JointAccountSignTask
    }
}