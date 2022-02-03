package com.tangem.tap.features.onboarding.products.wallet.redux

import android.graphics.Bitmap
import com.tangem.tap.common.redux.StateDialog
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val step: OnboardingWalletStep = OnboardingWalletStep.None,
    val backupState: BackupState = BackupState(),
    val cardArtworkUrl: String? = null,
    val showConfetti: Boolean = false,
) : StateType {

    fun getProgressStep(): Int {
        return when {
            step == OnboardingWalletStep.CreateWallet -> {
                1
            }
            step == OnboardingWalletStep.Backup -> {
                when (backupState.backupStep) {
                    BackupStep.InitBackup -> 2
                    BackupStep.ScanOriginCard -> 3

                    BackupStep.AddBackupCards -> 4
                    BackupStep.EnterAccessCode -> 4
                    BackupStep.ReenterAccessCode -> 4
                    BackupStep.SetAccessCode -> 4
                    BackupStep.WritePrimaryCard, is BackupStep.WriteBackupCard -> 5
                    BackupStep.Finished -> 6
                }
            }
            step == OnboardingWalletStep.Done -> 6
            else -> 1
        }
    }
}

enum class OnboardingWalletStep {
    None, CreateWallet, Backup, Done
}

data class BackupState(
    val primaryCardId: String? = null,
    val backupCardsNumber: Int = 0,
    val backupCardIds: List<CardId> = emptyList(),
    val backupCardsArtworks: Map<CardId, Bitmap> = emptyMap(),
    val accessCode: String = "",
    val accessCodeError: AccessCodeError? = null,
    val backupStep: BackupStep = BackupStep.InitBackup,
    val maxBackupCards: Int = 2,
    val canSkipBackup: Boolean = true,
    val buyAdditionalCardsUrl: String? = null
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

sealed class BackupDialog: StateDialog {
    object AddMoreBackupCards : StateDialog
    object BackupInProgress : StateDialog
    object UnfinishedBackupFound : StateDialog
    object ConfirmDiscardingBackup : StateDialog
}