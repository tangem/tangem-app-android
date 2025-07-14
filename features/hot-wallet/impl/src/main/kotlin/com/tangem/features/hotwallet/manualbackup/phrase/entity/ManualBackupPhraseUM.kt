package com.tangem.features.hotwallet.manualbackup.phrase.entity

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ManualBackupPhraseUM(
    val onContinueClick: () -> Unit,
    val words: ImmutableList<MnemonicGridItem> = persistentListOf(),
) {
    data class MnemonicGridItem(
        val index: Int,
        val mnemonic: String,
    )
}