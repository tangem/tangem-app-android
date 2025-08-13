package com.tangem.features.hotwallet.manualbackup.check.entity

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ManualBackupCheckUM(
    val onCompleteButtonClick: () -> Unit,
    val wordFields: ImmutableList<WordField>,
    val words: ImmutableList<String> = persistentListOf(),
    val completeButtonEnabled: Boolean,
    val completeButtonProgress: Boolean,
) {
    data class WordField(
        val index: Int,
        val word: TextFieldValue,
        val error: Boolean,
        val onChange: (TextFieldValue) -> Unit,
    )
}