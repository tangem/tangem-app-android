package com.tangem.tap.features.onboarding.products.wallet.redux

import android.graphics.Bitmap
import android.net.Uri
import com.tangem.common.CardFilter
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayActivationStep
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val step: OnboardingWalletStep = OnboardingWalletStep.None,
    val wallet2State: OnboardingWallet2State? = null,
    val backupState: BackupState = BackupState(),
    val onboardingSaltPayState: OnboardingSaltPayState? = null,
    val isSaltPay: Boolean = false,
    val cardArtworkUri: Uri? = null,
    val showConfetti: Boolean = false,
) : StateType {

    val backupCardIdFilter: CardFilter.Companion.CardIdFilter?
        get() = when {
            isSaltPay -> CardFilter.Companion.CardIdFilter.Allow(
                items = SaltPayWorkaround.walletCardIds.toSet(),
                ranges = SaltPayWorkaround.walletCardIdRanges,
            )
            else -> null
        }

    @Suppress("MagicNumber")
    fun getMaxProgress(): Int {
        val baseProgress = if (isSaltPay) 12 else 6
        return getWallet2Progress() + baseProgress
    }

    @Suppress("ComplexMethod", "MagicNumber")
    fun getProgressStep(): Int {
        val progressByStep = when {
            step == OnboardingWalletStep.CreateWallet -> 1
            step == OnboardingWalletStep.Backup -> {
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
            step == OnboardingWalletStep.SaltPay && isSaltPay -> {
                when (onboardingSaltPayState!!.step) {
                    SaltPayActivationStep.None, SaltPayActivationStep.NoGas -> 0
                    SaltPayActivationStep.NeedPin -> 7
                    SaltPayActivationStep.CardRegistration -> 8
                    SaltPayActivationStep.KycIntro, SaltPayActivationStep.KycStart,
                    SaltPayActivationStep.KycWaiting, SaltPayActivationStep.KycReject,
                    -> 9
                    SaltPayActivationStep.Claim -> 10
                    SaltPayActivationStep.ClaimInProgress -> 11
                    SaltPayActivationStep.ClaimSuccess, SaltPayActivationStep.Success,
                    -> getMaxProgress()
                }
            }
            step == OnboardingWalletStep.Done -> getMaxProgress()
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
    None, CreateWallet, SaltPay, Backup, Done
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
    object AddMoreBackupCards : BackupDialog()
    object BackupInProgress : BackupDialog()
    object UnfinishedBackupFound : BackupDialog()
    object ConfirmDiscardingBackup : BackupDialog()
    data class ResetBackupCard(val cardId: String) : BackupDialog()
}