package com.tangem.features.hotwallet.addexistingwallet.im.port.entity

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class AddExistingWalletImportUM(
    val words: TextFieldValue,
    val wordsChange: (TextFieldValue) -> Unit,
    val passPhrase: TextFieldValue,
    val passPhraseChange: (TextFieldValue) -> Unit,
    val onPassphraseInfoClick: () -> Unit,
    val wordsErrorText: TextReference?,
    val invalidWords: ImmutableList<String>,
    val importWalletEnabled: Boolean,
    val importWalletProgress: Boolean,
    val importWalletClick: () -> Unit,
    val suggestionsList: ImmutableList<String>,
    val onSuggestionClick: (String) -> Unit,
    val readyToImport: Boolean,
)