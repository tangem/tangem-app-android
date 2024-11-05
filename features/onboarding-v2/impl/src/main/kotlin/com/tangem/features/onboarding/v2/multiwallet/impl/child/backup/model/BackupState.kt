package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model

import android.graphics.Bitmap
import com.tangem.common.card.Card

// TODO maybe delete some fields in the future
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
