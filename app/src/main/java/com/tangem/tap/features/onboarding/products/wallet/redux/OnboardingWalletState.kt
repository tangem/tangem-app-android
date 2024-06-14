package com.tangem.tap.features.onboarding.products.wallet.redux

import android.graphics.Bitmap
import android.net.Uri
import com.tangem.domain.redux.StateDialog
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val step: OnboardingWalletStep = OnboardingWalletStep.None,
    val wallet2State: OnboardingWallet2State? = null,
    val backupState: BackupState = BackupState(),
    val cardArtworkUri: Uri? = null,
    val showConfetti: Boolean = false,
    val isRingOnboarding: Boolean = false,
) : StateType {

    @Suppress("MagicNumber")
    fun getMaxProgress(): Int {
        val baseProgress = 6
        return getWallet2Progress() + baseProgress
    }

    @Suppress("ComplexMethod", "MagicNumber")
    fun getProgressStep(): Int {
        val progressByStep = when (step) {
            OnboardingWalletStep.CreateWallet -> 1
            OnboardingWalletStep.Backup -> {
                when (backupState.backupStep) {
                    BackupStep.InitBackup -> 2
                    BackupStep.ScanOriginCard -> 3
                    BackupStep.AddBackupCards -> 4
                    BackupStep.EnterAccessCode -> 4
                    BackupStep.ReenterAccessCode -> 4
                    BackupStep.SetAccessCode -> 4
                    BackupStep.WritePrimaryCard, is BackupStep.WriteBackupCard -> 5
                    BackupStep.Finished -> getMaxProgress()
                }
            }
            OnboardingWalletStep.Done -> getMaxProgress()
            else -> 1
        }

        return getWallet2Progress() + progressByStep
    }

    private fun getWallet2Progress(): Int = wallet2State?.maxProgress ?: 0
}

data class OnboardingWallet2State(
    val maxProgress: Int,
)

enum class OnboardingWalletStep {
    None, CreateWallet, Backup, Done
}

data class BackupState(
    val primaryCardId: String? = null,
    val backupCardsNumber: Int = 0,
    val backupCardIds: List<CardId> = emptyList(),
    @Transient
    val backupCardsArtworks: Map<CardId, Bitmap> = emptyMap(),
    val accessCode: String = "",
    val accessCodeError: AccessCodeError? = null,
    val backupStep: BackupStep = BackupStep.InitBackup,
    val maxBackupCards: Int = 2,
    val canSkipBackup: Boolean = true,
    val isInterruptedBackup: Boolean = false,
    val showBtnLoading: Boolean = false,
    val hasBackupError: Boolean = false,
)

enum class AccessCodeError {
    CodeTooShort, CodesDoNotMatch
}

typealias CardId = String

sealed class BackupStep {
    object InitBackup : BackupStep()
    object ScanOriginCard : BackupStep()
    object AddBackupCards : BackupStep()
    object SetAccessCode : BackupStep()
    object EnterAccessCode : BackupStep()
    object ReenterAccessCode : BackupStep()
    object WritePrimaryCard : BackupStep()
    data class WriteBackupCard(val cardNumber: Int) : BackupStep()
    object Finished : BackupStep()
}

sealed class BackupDialog : StateDialog {
    object AttestationFailed : BackupDialog()
    object AddMoreBackupCards : BackupDialog()
    object BackupInProgress : BackupDialog()
    object UnfinishedBackupFound : BackupDialog()
    object ConfirmDiscardingBackup : BackupDialog()
    data class ResetBackupCard(val cardId: String) : BackupDialog()
}