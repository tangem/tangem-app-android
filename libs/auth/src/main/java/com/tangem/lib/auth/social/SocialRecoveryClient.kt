package com.tangem.lib.auth.social

import arrow.core.Either
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletSigner

/**
 * Transport half of social recovery: the provider identity ↔ wallet binding on the Tangem backend and
 * the recovery key `K` stored with it.
 *
 * Lives next to mobile authentication because every call rides on the device key, the DPoP-bound
 * session and the device metadata that already belong here. Callers only ever see plaintext
 * `K` — sealing it for the KMS on the way out and opening the HPKE envelope on the way in both happen
 * inside this layer.
 */
interface SocialRecoveryClient {

    /**
     * Server nonce the provider id token must be bound to, so a token obtained elsewhere cannot be
     * replayed here. The same value has to come back as the `nonce` of [recoverKey] — the
     * backend consumes it there.
     */
    suspend fun requestSignInNonce(): Either<SocialRecoveryError, SignInNonce>

    /**
     * Returns the recovery key stored for the identity behind [providerIdToken], which is also the
     * answer to whether this login creates or restores a wallet:
     * [SocialRecoveryError.BindingNotFound] means the identity has no wallet yet.
     *
     * The returned array is the caller's to wipe.
     *
     * @param nonce the [requestSignInNonce] value the id token was minted with.
     */
    suspend fun recoverKey(nonce: String, providerIdToken: String): Either<SocialRecoveryError, ByteArray>

    /**
     * Binds [walletId] to the identity behind [providerIdToken] and stores [recoveryKey] with it,
     * encrypted for the KMS public key.
     *
     * A fresh wallet nonce is fetched and signed on every call: the endpoint returns credentials, so
     * the exact same request can never be replayed. Re-registering a wallet already registered without
     * social fields updates the binding rather than creating a second one.
     */
    suspend fun registerBinding(
        walletId: String,
        providerIdToken: String,
        recoveryKey: ByteArray,
        signer: WalletSigner,
    ): Either<SocialRecoveryError, Unit>

    /**
     * Drops the binding of [walletId] and the recovery key stored with it, so the identity can no
     * longer restore this wallet. Unbinding a wallet that has no binding is a success.
     *
     * There is no binding-only endpoint: the wallet registration is what carries the binding, so this
     * unregisters the wallet. Fine for its only caller — the upgrade to a card keeps the wallet id, so
     * the scan behind it found the hot registration's marker and skipped the cold one; dropping that
     * marker here is what lets the next card tap register the wallet as cold.
     */
    suspend fun unbindWallet(walletId: String): Either<SocialRecoveryError, Unit>
}

/**
 * The two forms one server nonce is used in, which are not interchangeable.
 *
 * @property forProvider what the id token is minted with — the Base64 SHA-256 of the nonce, the hashing
 * both Apple and Google prescribe, and what the backend rebuilds to check the `nonce` claim
 * @property forRequest the nonce itself, for [SocialRecoveryClient.recoverKey] and everything bound to it
 * (the device signature, the attestation)
 */
data class SignInNonce(val forProvider: String, val forRequest: String)

sealed interface SocialRecoveryError {

    /** No wallet is bound to this provider identity — the caller must create one, not show an error */
    data object BindingNotFound : SocialRecoveryError

    /** The identity already points at another wallet, or the wallet at another identity */
    data object BindingConflict : SocialRecoveryError

    /** No authenticated session to run the call on; device registration has to happen first */
    data object NoActiveSession : SocialRecoveryError

    data object DeviceKeyUnavailable : SocialRecoveryError

    data class SigningFailed(val cause: Throwable? = null) : SocialRecoveryError

    /** The HPKE envelope did not open — a wrong ephemeral key, a truncated response or a tampered one */
    data class DecryptionFailed(val cause: Throwable? = null) : SocialRecoveryError

    data class Api(val error: AuthError) : SocialRecoveryError

    data class Unknown(val cause: Throwable? = null) : SocialRecoveryError
}