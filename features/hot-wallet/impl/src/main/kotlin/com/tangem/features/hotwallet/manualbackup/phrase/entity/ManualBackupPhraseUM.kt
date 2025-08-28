package com.tangem.features.hotwallet.manualbackup.phrase.entity

import com.tangem.core.ui.components.grid.entity.SeedPhraseGridItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ManualBackupPhraseUM(
    val onContinueClick: () -> Unit,
    val words: ImmutableList<SeedPhraseGridItem> = persistentListOf(),
)