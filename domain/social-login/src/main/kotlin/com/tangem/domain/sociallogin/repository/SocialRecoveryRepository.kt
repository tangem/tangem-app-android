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
 * Every call runs on top of the mobile authentication session. Sealing the key for the KMS on
 * the way out and opening it on the way in both happen below this interface, so callers only ever see
 * a plaintext [RecoveryKey].
 */
interface SocialRecoveryRepository {

    /**
     * Returns the recovery key stored for the identity behind [idToken], which is also the answer to
     * whether this login creates or restores a wallet: [SocialLoginError.BindingNotFound] means the
     * identity has no wallet yet and the caller must create one, not show an error.
     */
    suspend fun getRecoveryKey(idToken: SocialProviderIdToken): Either<SocialLoginError, RecoveryKey>

    /**
     * Binds [walletId] to the identity behind [idToken] and stores [recoveryKey] encrypted for it.
     *
     * Idempotent per identity: repeating the call after a network failure re-uses the existing binding
     * instead of creating a second one. Fails with [SocialLoginError.BindingConflict] when the
     * identity or the wallet is already bound to a different counterpart.
     */
    suspend fun registerRecoveryKey(
        idToken: SocialProviderIdToken,
        walletId: UserWalletId,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit>

    /**
     * Drops the binding of [walletId] and the recovery key stored with it, so the identity can no longer
     * restore this wallet. Used after an upgrade to a hardware wallet. Unbinding a wallet that
     * has no binding is a success.
     */
    suspend fun unbindWallet(walletId: UserWalletId): Either<SocialLoginError, Unit>
}