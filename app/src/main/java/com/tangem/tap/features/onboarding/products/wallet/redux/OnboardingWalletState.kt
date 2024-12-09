package com.tangem.tap.features.onboarding.products.wallet.redux

import android.graphics.Bitmap
import android.net.Uri
import com.tangem.common.card.Card
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.wallets.models.UserWalletId
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val step: OnboardingWalletStep = OnboardingWalletStep.None,
    val wallet2State: OnboardingWallet2State? = null,
    val backupState: BackupState = BackupState(),
    val walletImages: WalletImages = WalletImages(),
    val showConfetti: Boolean = false,
    val isRingOnboarding: Boolean = false,
    val userWalletId: UserWalletId? = null,
) : StateType {

    @Suppress("MagicNumber")
    fun getMaxProgress(): Int {
        val baseProgress = 7
        return getWallet2Progress() + baseProgress
    }

    @Suppress("ComplexMethod", "MagicNumber")
    fun getProgressStep(): Int {
        val progressByStep = when (step) {
            OnboardingWalletStep.CreateWallet, OnboardingWalletStep.None -> 1
            OnboardingWalletStep.Backup -> {
                when (backupState.backupStep) {
                    null -> 2
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
            OnboardingWalletStep.ManageTokens -> 6
            OnboardingWalletStep.Done -> getMaxProgress()
        }

        return getWallet2Progress() + progressByStep
    }

    private fun getWallet2Progress(): Int = wallet2State?.maxProgress ?: 0
}

data class WalletImages(
    val primaryCardImage: Uri? = null,
    val secondCardImage: Uri? = null,
    val thirdCardImage: Uri? = null,
)

data class OnboardingWallet2State(
    val maxProgress: Int,
)

enum class OnboardingWalletStep {
    None, CreateWallet, Backup, ManageTokens, Done
}

data class BackupState(
    val primaryCardId: String? = null,
    val primaryCardBatchId: String? = null,
    val backupCardsNumber: Int = 0,
    val backupCards: List<Card> = emptyList(),
    val backupCardIds: List<CardId> = emptyList(),
    val backupCardBatchIds: List<CardId> = emptyList(),
    @Transient
    val backupCardsArtworks: Map<CardId, Bitmap> = emptyMap(),
    val accessCode: String = "",
    val accessCodeError: AccessCodeError? = null,
    val backupStep: BackupStep? = null,
    val maxBackupCards: Int = 2,
    val canSkipBackup: Boolean = true,
    val isInterruptedBackup: Boolean = false,
    val showBtnLoading: Boolean = false,
    val hasBackupError: Boolean = false,
    val startedSource: BackupStartedSource = BackupStartedSource.Onboarding,
    val hasRing: Boolean = false,
)

enum class BackupStartedSource {
    Onboarding, CreateBackup
}

enum class AccessCodeError {
    CodeTooShort, CodesDoNotMatch
}

typealias CardId = String

sealed class BackupStep {
    data object InitBackup : BackupStep()
    data object ScanOriginCard : BackupStep()
    data object AddBackupCards : BackupStep()
    data object SetAccessCode : BackupStep()
    data object EnterAccessCode : BackupStep()
    data object ReenterAccessCode : BackupStep()
    data object WritePrimaryCard : BackupStep()
    data class WriteBackupCard(val cardNumber: Int) : BackupStep()
    data object Finished : BackupStep()
}

sealed class BackupDialog : StateDialog {
    data object AttestationFailed : BackupDialog()
    data object AddMoreBackupCards : BackupDialog()
    data object BackupInProgress : BackupDialog()

    data class UnfinishedBackupFound(
        val scanResponse: ScanResponse? = null,
    ) : BackupDialog()

    data class ConfirmDiscardingBackup(
        val scanResponse: ScanResponse? = null,
    ) : BackupDialog()

    data class ResetBackupCard(val cardId: String) : BackupDialog()
}