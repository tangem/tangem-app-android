package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model

import com.tangem.domain.models.scan.CardDTO

data class BackupState(
    val addedCards: List<CardDTO> = emptyList(),
) {

    val numberOfBackupCards: Int
        get() = addedCards.size
}