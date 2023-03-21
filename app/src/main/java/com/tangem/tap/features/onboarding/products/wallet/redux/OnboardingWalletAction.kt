package com.tangem.tap.features.onboarding.products.wallet.redux

import android.net.Uri
import org.rekotlin.Action

sealed class OnboardingWalletAction : Action {
    object Init : OnboardingWalletAction()
    object GetToCreateWalletStep : OnboardingWalletAction()
    object GetToSaltPayStep : OnboardingWalletAction()
    object CreateWallet : OnboardingWalletAction()
    object Done : OnboardingWalletAction()
    object FinishOnboarding : OnboardingWalletAction()

    object ResumeBackup : OnboardingWalletAction()

    data class LoadArtwork(val cardArtworkUriForUnfinishedBackup: Uri? = null) : OnboardingWalletAction()
    class SetArtworkUrl(val artworkUri: Uri?) : OnboardingWalletAction()

    object OnBackPressed : OnboardingWalletAction()
}

sealed class BackupAction : Action {

    object IntroduceBackup : BackupAction()
    object StartBackup : BackupAction()
    object SkipBackup : BackupAction()

    object StartAddingPrimaryCard : BackupAction()
    object ScanPrimaryCard : BackupAction()

    /**
     * Check for unfinished backup of standard Wallets and SaltPay cards
     * See more GlobalAction.Onboarding.StartForUnfinishedBackup
     */
    object CheckForUnfinishedBackup : BackupAction()

    object StartAddingBackupCards : BackupAction()
    object AddBackupCard : BackupAction() {
        object Success : BackupAction()
    }

    object FinishAddingBackupCards : BackupAction()

    object ShowAccessCodeInfoScreen : BackupAction()
    object ShowEnterAccessCodeScreen : BackupAction()
    data class CheckAccessCode(val accessCode: String) : BackupAction()
    data class SetAccessCodeError(val error: AccessCodeError?) : BackupAction()
    data class SaveFirstAccessCode(val accessCode: String) : BackupAction()
    data class SaveAccessCodeConfirmation(val accessCodeConfirmation: String) : BackupAction()
    object OnAccessCodeDialogClosed : BackupAction()

    object PrepareToWritePrimaryCard : BackupAction()
    object WritePrimaryCard : BackupAction()
    data class PrepareToWriteBackupCard(val cardNumber: Int) : BackupAction()
    data class WriteBackupCard(val cardNumber: Int) : BackupAction()

    /**
     * It always calls for the SaltPay cards when resuming the activation process.
     */
    data class FinishBackup(val withAnalytics: Boolean = true) : BackupAction()

    object DiscardBackup : BackupAction()
    object DiscardSavedBackup : BackupAction()
    object ResumeFoundUnfinishedBackup : BackupAction()
}
