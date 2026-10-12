package com.tangem.features.hotwallet

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider

/**
 * Scenarios 1 and 2 of UC-08 behind one call: sign in, then create a wallet or restore the existing one.
 *
 * The backend decides which, not the caller — the same entry point serves both "Create wallet" and
 * "I have a wallet" (FR-02).
 */
interface SocialLoginFlow {

    /** @param onStageChanged called on the flow's own coroutine, never after it returns */
    suspend fun signIn(
        provider: SocialLoginProvider,
        onStageChanged: (SocialLoginStage) -> Unit = {},
    ): Either<SocialLoginError, SocialLoginOutcome>

    /** Forgets the picked account, so the next [signIn] shows the chooser again ("switch account") */
    suspend fun signOut(provider: SocialLoginProvider)
}

enum class SocialLoginStage {

    /** Waiting on the provider and on the backend; the branch is not decided yet */
    SigningIn,

    /** Scenario 1: the identity is new, so a wallet is created, backed up and bound to it */
    CreatingWallet,

    /** Scenario 2: the identity already owns a wallet, restored from the cloud backup */
    RestoringWallet,
}

/** Both outcomes leave a usable wallet on the device */
sealed interface SocialLoginOutcome {

    val userWalletId: UserWalletId

    data class WalletCreated(override val userWalletId: UserWalletId) : SocialLoginOutcome

    data class WalletRestored(override val userWalletId: UserWalletId) : SocialLoginOutcome
}