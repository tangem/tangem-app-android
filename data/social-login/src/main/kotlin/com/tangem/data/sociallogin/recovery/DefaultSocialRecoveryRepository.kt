package com.tangem.data.sociallogin.recovery

import android.util.Base64
import arrow.core.Either
import arrow.core.flatMap
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletSignerProvider
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.SocialRecoveryError
import javax.inject.Inject

/**
 * Domain-facing half of social recovery: everything transport-shaped lives in the auth library, this
 * only translates wallet ids and the failure vocabulary.
 */
internal class DefaultSocialRecoveryRepository @Inject constructor(
    private val client: SocialRecoveryClient,
    private val walletSignerProvider: WalletSignerProvider,
) : SocialRecoveryRepository {

    override suspend fun getRecoveryKey(idToken: SocialProviderIdToken): Either<SocialLoginError, RecoveryKey> =
        client.recoverKey(nonce = idToken.nonce, providerIdToken = idToken.value)
            .mapLeft(SocialRecoveryError::toDomainError)
            .flatMap { keyBytes ->
                // a key of the wrong length means the envelope opened into something that is not K
                Either.catch { RecoveryKey(keyBytes) }
                    .mapLeft { SocialLoginError.RecoveryKeyDecryptionFailed(it) }
            }

    override suspend fun registerRecoveryKey(
        idToken: SocialProviderIdToken,
        walletId: UserWalletId,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> {
        val backendWalletId = walletId.toBackendId()
        val signer = walletSignerProvider.signerFor(backendWalletId)
            ?: return Either.Left(SocialLoginError.ServerError())

        return client.registerBinding(
            walletId = backendWalletId,
            providerIdToken = idToken.value,
            recoveryKey = recoveryKey.value,
            signer = signer,
        ).mapLeft(SocialRecoveryError::toDomainError)
    }

    override suspend fun unbindWallet(walletId: UserWalletId): Either<SocialLoginError, Unit> =
        client.unbindWallet(walletId.toBackendId()).mapLeft(SocialRecoveryError::toDomainError)
}

/**
 * The auth service addresses wallets by the Base64 form of [UserWalletId], the same one wallet
 * registration uses — a hex id would not match the registration the wallet already has.
 */
private fun UserWalletId.toBackendId(): String = Base64.encodeToString(value, Base64.NO_WRAP)

private fun SocialRecoveryError.toDomainError(): SocialLoginError = when (this) {
    SocialRecoveryError.BindingNotFound -> SocialLoginError.BindingNotFound
    SocialRecoveryError.BindingConflict -> SocialLoginError.BindingConflict
    SocialRecoveryError.NoActiveSession,
    SocialRecoveryError.DeviceKeyUnavailable,
    -> SocialLoginError.ServerError()
    is SocialRecoveryError.SigningFailed -> SocialLoginError.ServerError(cause)
    is SocialRecoveryError.DecryptionFailed -> SocialLoginError.RecoveryKeyDecryptionFailed(cause)
    is SocialRecoveryError.Api -> when (error) {
        AuthError.NetworkError -> SocialLoginError.NetworkError
        else -> SocialLoginError.ServerError()
    }
    is SocialRecoveryError.Unknown -> SocialLoginError.Unknown(cause)
}