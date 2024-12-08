package com.tangem.feature.onboarding.presentation.wallet2.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.ButtonState
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.DescriptionSubTitleText
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.InvalidWordsColorTransformation
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseErrorConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
@Composable
fun ImportSeedPhraseScreen(state: ImportSeedPhraseState, modifier: Modifier = Modifier) {
    val keyboard by keyboardAsState()
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column {
            OnboardingDescriptionBlock(
                modifier = Modifier.padding(top = TangemTheme.dimens.size16),
            ) {
                DescriptionSubTitleText(text = stringResourceSafe(id = R.string.onboarding_seed_import_message))
            }
            PhraseBlock(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.size62)
                    .padding(horizontal = TangemTheme.dimens.size16),
                state = state,
            )

            PassphraseBlock(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.size16),
                passphraseField = state.fieldPassphrase,
                onPassphraseInfoClick = state.onPassphraseInfoClick,
            )

            OnboardingActionBlock(
                modifier = Modifier.padding(top = TangemTheme.dimens.size16),
                firstActionContent = {
                    PrimaryButtonIconEnd(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResourceSafe(id = R.string.common_import),
                        iconResId = R.drawable.ic_tangem_24,
                        enabled = state.buttonCreateWallet.enabled,
                        showProgress = state.buttonCreateWallet.showProgress,
                        onClick = state.buttonCreateWallet.onClick,
                    )
                },
            )
        }

        if (keyboard is Keyboard.Opened) {
            SuggestionsBlock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
                suggestionsList = state.suggestionsList,
                onClick = { index -> state.onSuggestedPhraseClick(index) },
            )
        }

        if (state.bottomSheetConfig != null) {
            PassphraseInfoBottomSheet(config = state.bottomSheetConfig)
        }
    }
}

@Composable
private fun PassphraseBlock(
    passphraseField: TextFieldState,
    onPassphraseInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlineTextFieldWithIcon(
        modifier = modifier.fillMaxWidth(),
        value = passphraseField.textFieldValue,
        onValueChange = passphraseField.onTextFieldValueChanged,
        iconResId = R.drawable.ic_information_24,
        iconColor = TangemTheme.colors.icon.informative,
        label = stringResourceSafe(id = R.string.common_passphrase),
        placeholder = stringResourceSafe(id = R.string.send_optional_field),
        onIconClick = onPassphraseInfoClick,
    )
}

@Composable
private fun PhraseBlock(state: ImportSeedPhraseState, modifier: Modifier = Modifier) {
    // TODO: replace by TextReference
    val errorConverter = remember { SeedPhraseErrorConverter() }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size142),
            value = state.fieldSeedPhrase.textFieldValue,
            onValueChange = state.fieldSeedPhrase.onTextFieldValueChanged,
            textStyle = TangemTheme.typography.body1,
            singleLine = false,
            visualTransformation = InvalidWordsColorTransformation(
                wordsToBrush = state.invalidWords,
                style = SpanStyle(color = TangemTheme.colors.text.warning),
            ),
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size32),
        ) {
            val message = errorConverter.convert(LocalContext.current to state.error)
            if (message != null) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    text = message,
                    style = TangemTheme.typography.caption2.copy(
                        color = TangemTheme.colors.text.warning,
                    ),
                )
            }
        }
    }
}

@Suppress("ReusedModifierInstance")
@Composable
private fun SuggestionsBlock(
    suggestionsList: ImmutableList<String>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        enter = fadeIn() + slideIn(initialOffset = { IntOffset(x = 200, y = 0) }),
        exit = slideOut(targetOffset = { IntOffset(x = -200, y = 0) }) + fadeOut(),
        visible = suggestionsList.isNotEmpty(),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.size16),
        ) {
            items(suggestionsList.size) { index ->
                SuggestionButton(
                    modifier = Modifier.rowPadding(
                        index = index,
                        rowSize = suggestionsList.size,
                        outSide = TangemTheme.dimens.size0,
                        inSide = TangemTheme.dimens.size4,
                    ),
                    text = suggestionsList[index],
                    onClick = { onClick(index) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        Notifier(
            text = text,
            backgroundColor = TangemTheme.colors.icon.primary1,
            textColor = TangemTheme.colors.text.primary2,
        )
    }
}

private fun Modifier.rowPadding(index: Int, rowSize: Int, outSide: Dp, inSide: Dp): Modifier = when (index) {
    0 -> this.padding(start = outSide, end = inSide)
    rowSize - 1 -> this.padding(start = inSide, end = outSide)
    else -> this.padding(horizontal = inSide)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuggestionsBlockPreview(
    @PreviewParameter(SuggestionsPreviewParamsProvider::class) suggestions: ImmutableList<String>,
) {
    TangemThemePreview {
        SuggestionsBlock(
            suggestionsList = suggestions,
            onClick = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportSeedPhraseScreenPreview(
    @PreviewParameter(SuggestionsPreviewParamsProvider::class) suggestions: ImmutableList<String>,
) {
    TangemThemePreview {
        Box(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            ImportSeedPhraseScreen(
                ImportSeedPhraseState(
                    fieldSeedPhrase = TextFieldState(onTextFieldValueChanged = {}),
                    fieldPassphrase = TextFieldState(onTextFieldValueChanged = {}),
                    onSuggestedPhraseClick = {},
                    onPassphraseInfoClick = {},
                    buttonCreateWallet = ButtonState(
                        enabled = true,
                        isClickable = true,
                        showProgress = false,
                        onClick = {},
                    ),
                    suggestionsList = suggestions,
                ),
            )
        }
    }
}

private class SuggestionsPreviewParamsProvider : CollectionPreviewParameterProvider<ImmutableList<String>>(
    collection = listOf(
        persistentListOf(
            "one",
            "each",
            "explorer",
            "unknown",
        ),
    ),
)