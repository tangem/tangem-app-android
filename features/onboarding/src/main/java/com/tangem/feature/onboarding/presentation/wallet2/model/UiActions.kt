package com.tangem.feature.onboarding.presentation.wallet2.model

import androidx.compose.ui.text.input.TextFieldValue

/**
* [REDACTED_AUTHOR]
 */
data class UiActions(
    val introActions: IntroUiAction,
    val aboutActions: AboutUiAction,
    val yourSeedPhraseActions: YourSeedPhraseUiAction,
    val checkSeedPhraseActions: CheckSeedPhraseUiAction,
    val importSeedPhraseActions: ImportSeedPhraseUiAction,
    val menuChatClick: () -> Unit,
    val menuNavigateBackClick: () -> Unit,
)

data class IntroUiAction(
    val buttonCreateWalletClick: () -> Unit,
    val buttonOtherOptionsClick: () -> Unit,
)

data class AboutUiAction(
    val buttonReadMoreAboutSeedPhraseClick: () -> Unit,
    val buttonGenerateSeedPhraseClick: () -> Unit,
    val buttonImportSeedPhraseClick: () -> Unit,
)

data class YourSeedPhraseUiAction(
    val buttonContinueClick: () -> Unit,
)

data class CheckSeedPhraseUiAction(
    val secondTextFieldAction: TextFieldUiAction,
    val seventhTextFieldAction: TextFieldUiAction,
    val eleventhTextFieldAction: TextFieldUiAction,
    val buttonCreateWalletClick: () -> Unit,
)

data class ImportSeedPhraseUiAction(
    val phraseTextFieldAction: TextFieldUiAction,
    val suggestedPhraseClick: (Int) -> Unit,
    val buttonCreateWalletClick: () -> Unit,
)

data class TextFieldUiAction(
    val onTextFieldChanged: (TextFieldValue) -> Unit,
    val onFocusChanged: (Boolean) -> Unit = {},
)

/**
* [REDACTED_TODO_COMMENT]

@MamaLemon MamaLemon 4 days ago
А для чего используется дефолтная инициализация?

Member
Author
@gbixahue gbixahue 4 days ago
Для того, чтобы не прописывать ее при инициализации, если это не нужно

Member
@MamaLemon MamaLemon 4 days ago
Если параметр не нужен для какого-то стейта, то этого параметра у него быть не должно

Member
Author
@gbixahue gbixahue 4 days ago •
Это переиспользуемая модель. В одном стейте параметр используется, в другом нет

Member
@MamaLemon MamaLemon 3 days ago
В таком случае стейт экрана перестает быть "чистым". В правильном виде каждый стейт экрана содержит только тот набор свойств, который ему реально нужен. Иначе будет трудно понять, какой стейт и что юзает.

Member
Author
@gbixahue gbixahue 2 days ago
Т.е. если у тебя есть поля
1 - можно вводить текст
2 - можно перехватывать фокус
3 - можно вводить текст и перехватывать фокус
4 - можно вводить текст, перехватывать фокус и что-нибудь еще

ты будешь для этого создавать 4 модели?
 */

enum class SeedPhraseField(val index: Int) {
    Second(index = 1),
    Seventh(index = 6),
    Eleventh(index = 10),
}
