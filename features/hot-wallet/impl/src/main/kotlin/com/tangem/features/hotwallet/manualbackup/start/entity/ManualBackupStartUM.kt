package com.tangem.features.hotwallet.manualbackup.start.entity

internal data class ManualBackupStartUM(
    val seepPhraseLength: Int = 12,
    val onContinueClick: () -> Unit,
)