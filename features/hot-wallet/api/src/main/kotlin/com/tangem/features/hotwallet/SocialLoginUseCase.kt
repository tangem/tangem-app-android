package com.tangem.features.hotwallet

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider

/**
 * Social login behind one call: sign in, then create a wallet or restore the existing one.
 *
 * The backend decides which, not the caller — the same entry point serves both "Create wallet" and
 * "I have a wallet".
 */
interface SocialLoginUseCase {

    /** @param onStageChanged called on the calling coroutine, never after signIn returns */
    suspend fun signIn(
        provider: SocialLoginProvider,
        onStageChanged: (SocialLoginStage) -> Unit = {},
    ): Either<SocialLoginError, SocialLoginOutcome>

    /** Forgets the picked account, so the next [signIn] shows the chooser again ("switch account") */
    suspend fun signOut(provider: SocialLoginProvider)
}

enum class SocialLoginStage {

    /** The provider owns the screen here — its account chooser and grant sheet */
    SigningIn,

    /** Asking the backend whether this identity already owns a wallet */
    VerifyingAccount,

    /** The backup is being downloaded and decrypted */
    PreparingWallet,

    CreatingWallet,

    RestoringWallet,
}

sealed interface SocialLoginOutcome {

    val userWalletId: UserWalletId

    data class WalletCreated(override val userWalletId: UserWalletId) : SocialLoginOutcome

    data class WalletRestored(override val userWalletId: UserWalletId) : SocialLoginOutcome
}