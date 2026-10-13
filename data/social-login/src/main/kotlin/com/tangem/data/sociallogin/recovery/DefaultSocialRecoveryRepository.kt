package com.tangem.data.sociallogin.recovery

import android.util.Base64
import arrow.core.Either
import arrow.core.flatMap
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy
import javax.inject.Inject

/**
 * Domain-facing half of social recovery: everything transport-shaped lives in the auth library, this
 * only translates wallet ids and the failure vocabulary.
 */
internal class DefaultSocialRecoveryRepository @Inject constructor(
    private val client: SocialRecoveryClient,
    private val hotWalletSignerFactory: HotWalletSignerFactory,
    // the wallets list reaches this repository through IsWalletBackedUpUseCase
    private val userWalletsListRepository: Lazy<UserWalletsListRepository>,
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
        userWallet: UserWallet.Hot,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> = client.registerBinding(
        walletId = userWallet.walletId.toBackendId(),
        providerIdToken = idToken.value,
        recoveryKey = recoveryKey.value,
        signer = hotWalletSignerFactory.signerFor(userWallet),
    ).mapLeft(SocialRecoveryError::toDomainError)

    override suspend fun registerRestoredWallet(userWallet: UserWallet.Hot): Either<SocialLoginError, Unit> =
        client.registerWallet(
            walletId = userWallet.walletId.toBackendId(),
            signer = hotWalletSignerFactory.signerFor(userWallet),
        ).mapLeft(SocialRecoveryError::toDomainError)

    override suspend fun unbindWallet(walletId: UserWalletId): Either<SocialLoginError, Unit> {
        val backendId = walletId.toBackendId()
        return client.unbindWallet(backendId).fold(
            ifRight = { Either.Right(Unit) },
            ifLeft = { error ->
                if (error != SocialRecoveryError.WalletNotRegistered) return Either.Left(error.toDomainError())
                // A wallet that only lives in the Hot SDK was never registered, and a binding is only ever

                // may have missed its registration on this device, and only that registration lets the
                // backend erase the binding.
                val userWallet = findListedHotWallet(walletId)
                    .getOrElse { return Either.Left(error.toDomainError()) }
                    ?: return Either.Right(Unit)
                client.registerWallet(walletId = backendId, signer = hotWalletSignerFactory.signerFor(userWallet))
                    .flatMap { client.unbindWallet(backendId) }
                    .mapLeft(SocialRecoveryError::toDomainError)
            },
        )
    }

    // an unreadable list must not pass for "not listed", or the binding of a listed wallet would be kept
    private suspend fun findListedHotWallet(walletId: UserWalletId): Result<UserWallet.Hot?> = runSuspendCatching {
        userWalletsListRepository.get().userWalletsSync().firstOrNull { it.walletId == walletId } as? UserWallet.Hot
    }.onFailure { TangemLogger.e("Failed to read the wallet list to release a social recovery binding", it) }
}

/**
 * The auth service addresses wallets by the Base64 form of [UserWalletId], the same one wallet
 * registration uses — a hex id would not match the registration the wallet already has.
 */
private fun UserWalletId.toBackendId(): String = Base64.encodeToString(value, Base64.NO_WRAP)