package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.tangem.core.ui.components.Notifier
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.core.ui.components.TangemTextFieldsDefault
import com.tangem.core.ui.components.buttons.common.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.DescriptionSubTitleText
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.InvalidWordsColorTransformation
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseErrorConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun ImportSeedPhraseScreen(state: ImportSeedPhraseState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            Column {
                OnboardingDescriptionBlock(
                    modifier = Modifier.padding(top = TangemTheme.dimens.size16),
                ) {
                    DescriptionSubTitleText(text = stringResource(id = R.string.onboarding_seed_import_message))
                }
                PhraseBlock(
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.size62)
                        .padding(horizontal = TangemTheme.dimens.size16),
                    state = state,
                )
                SuggestionsBlock(
                    modifier = Modifier.padding(top = TangemTheme.dimens.size4),
                    suggestionsList = state.suggestionsList,
                    onClick = { index -> state.onSuggestedPhraseClick(index) },
                )
            }
        }
        Box {
            OnboardingActionBlock(
                firstActionContent = {
                    PrimaryButtonIconStart(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.onboarding_create_wallet_button_create_wallet),
                        iconResId = R.drawable.ic_tangem_24,
                        enabled = state.buttonCreateWallet.enabled,
                        showProgress = state.buttonCreateWallet.showProgress,
                        onClick = state.buttonCreateWallet.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun PhraseBlock(state: ImportSeedPhraseState, modifier: Modifier = Modifier) {
// [REDACTED_TODO_COMMENT]
    val errorConverter = remember { SeedPhraseErrorConverter() }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size142),
            value = state.tvSeedPhrase.textFieldValue,
            onValueChange = state.tvSeedPhrase.onTextFieldValueChanged,
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
                    style = TangemTheme.typography.caption.copy(
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
        enter = fadeIn() + slideIn(initialOffset = { IntOffset(x = 200, y = 0) }),
        exit = slideOut(targetOffset = { IntOffset(x = -200, y = 0) }) + fadeOut(),
        visible = suggestionsList.isNotEmpty(),
    ) {
        LazyRow(
            modifier = modifier,
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
            backgroundColor = TangemTheme.colors.background.action,
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
@Composable
private fun SuggestionsBlockPreview_Light(
    @PreviewParameter(SuggestionsPreviewParamsProvider::class) suggestions: ImmutableList<String>,
) {
    TangemTheme(isDark = false) {
        SuggestionsBlock(
            suggestionsList = suggestions,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SuggestionsBlockPreview_Dark(
    @PreviewParameter(SuggestionsPreviewParamsProvider::class) suggestions: ImmutableList<String>,
) {
    TangemTheme(isDark = true) {
        SuggestionsBlock(
            suggestionsList = suggestions,
            onClick = {},
        )
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
