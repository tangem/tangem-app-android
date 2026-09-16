package com.tangem.domain.sociallogin.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError

/**
 * The backend half of social recovery: the binding between a provider identity and a wallet, and the
 * recovery key stored alongside it.
 *
 * Every call runs on top of the mobile authentication session (FR-07) and carries the device key, which
 * is what the backend re-encrypts the recovery key for. Encrypting the key with the KMS public key on
 * the way out and decrypting it with the device private key on the way in both happen inside this
 * layer, so callers only ever see plaintext [RecoveryKey] (FR-04, FR-08).
 */
interface SocialRecoveryRepository {

    /**
     * Returns the recovery key stored for the identity behind [idToken], which is also the answer to
     * whether this login creates or restores a wallet: [SocialLoginError.BindingNotFound] means the
     * identity has no wallet yet and the caller must create one, not show an error (FR-02).
     */
    suspend fun getRecoveryKey(idToken: SocialProviderIdToken): Either<SocialLoginError, RecoveryKey>

    /**
     * Binds [walletId] to the identity behind [idToken] and stores [recoveryKey] encrypted for it.
     *
     * Idempotent per identity: repeating the call after a network failure re-uses the existing binding
     * instead of creating a second one (NFR-02). Fails with [SocialLoginError.BindingConflict] when the
     * identity or the wallet is already bound to a different counterpart (FR-14).
     */
    suspend fun registerRecoveryKey(
        idToken: SocialProviderIdToken,
        walletId: UserWalletId,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit>

    /**
     * Drops the binding of [walletId] and the recovery key stored with it, so the identity can no longer
     * restore this wallet. Used after an upgrade to a hardware wallet (FR-16). Unbinding a wallet that
     * has no binding is a success.
     */
    suspend fun unbindWallet(walletId: UserWalletId): Either<SocialLoginError, Unit>
}