package com.tangem.features.hotwallet.manualbackup.completed.entity

import com.tangem.core.ui.extensions.TextReference

internal data class ManualBackupCompletedUM(
    val title: TextReference,
    val description: TextReference,
    val onContinueClick: () -> Unit,
    val isLoading: Boolean,
)