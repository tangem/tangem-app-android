package com.tangem.tap.features.onboarding.products.wallet.redux

import android.net.Uri
import com.tangem.common.CompletionResult
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse
import kotlinx.coroutines.CoroutineScope
import org.rekotlin.Action

sealed class OnboardingWalletAction : Action {
    data object Init : OnboardingWalletAction()
    data object GetToCreateWalletStep : OnboardingWalletAction()
    data object CreateWallet : OnboardingWalletAction()
    data class WalletWasCreated(
        val shouldSendAnalyticsEvent: Boolean,
        val result: CompletionResult<CreateProductWalletTaskResponse>,
    ) : OnboardingWalletAction()

    data object Done : OnboardingWalletAction()
    data class FinishOnboarding(val scope: CoroutineScope) : OnboardingWalletAction()

    data object ResumeBackup : OnboardingWalletAction()

    data class LoadArtwork(val cardArtworkUriForUnfinishedBackup: Uri? = null) : OnboardingWalletAction()
    class SetArtworkUrl(val artworkUri: Uri?) : OnboardingWalletAction()

    data object OnBackPressed : OnboardingWalletAction()
}

sealed class OnboardingWallet2Action : OnboardingWalletAction() {
    data class Init(val maxProgress: Int) : OnboardingWallet2Action()
    data class SetDependencies(val maxProgress: Int) : OnboardingWallet2Action()

    data class CreateWallet(
        val callback: (CompletionResult<CreateWalletResponse>) -> Unit,
    ) : OnboardingWallet2Action()

    data class ImportWallet(
        val mnemonicComponents: List<String>,
        val passphrase: String?,
        val seedPhraseSource: SeedPhraseSource,
        val callback: (CompletionResult<CreateWalletResponse>) -> Unit,
    ) : OnboardingWallet2Action()

    data class WalletWasCreated(
        val result: CompletionResult<CreateWalletResponse>,
    ) : OnboardingWallet2Action()
}

sealed class BackupAction : Action {

    data object IntroduceBackup : BackupAction()
    data object StartBackup : BackupAction()
    data object SkipBackup : BackupAction()

    data object ErrorInBackupCard : BackupAction()
    data object StartAddingPrimaryCard : BackupAction()
    data object ScanPrimaryCard : BackupAction()

    /**
     * Check for unfinished backup of standard Wallets
     * See more GlobalAction.Onboarding.StartForUnfinishedBackup
     */
    data object CheckForUnfinishedBackup : BackupAction()

    data object StartAddingBackupCards : BackupAction()
    data object AddBackupCard : BackupAction() {
        data object Success : BackupAction()
        data class ChangeButtonLoading(val isLoading: Boolean) : BackupAction()
    }

    data object FinishAddingBackupCards : BackupAction()

    data object ShowAccessCodeInfoScreen : BackupAction()
    data object ShowEnterAccessCodeScreen : BackupAction()
    data class CheckAccessCode(val accessCode: String) : BackupAction()
    data class SetAccessCodeError(val error: AccessCodeError?) : BackupAction()
    data class SaveFirstAccessCode(val accessCode: String) : BackupAction()
    data class SaveAccessCodeConfirmation(val accessCodeConfirmation: String) : BackupAction()
    data object OnAccessCodeDialogClosed : BackupAction()

    data object PrepareToWritePrimaryCard : BackupAction()
    data object WritePrimaryCard : BackupAction()
    data class PrepareToWriteBackupCard(val cardNumber: Int) : BackupAction()
    data class WriteBackupCard(val cardNumber: Int) : BackupAction()

    data class FinishBackup(val withAnalytics: Boolean = true) : BackupAction()

    data object DiscardBackup : BackupAction()
    data object DiscardSavedBackup : BackupAction()
    data object ResumeFoundUnfinishedBackup : BackupAction()

    data class ResetBackupCard(val cardId: String) : BackupAction()
}
