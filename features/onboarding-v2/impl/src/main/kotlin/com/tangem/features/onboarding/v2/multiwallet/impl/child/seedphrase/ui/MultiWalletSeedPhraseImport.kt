package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.utils.InvalidWordsColorTransformation
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod")
@Composable
internal fun MultiWalletSeedPhraseImport(state: MultiWalletSeedPhraseUM.Import, modifier: Modifier = Modifier) {
    if (state.dialog != null) {
        BasicDialog(
            title = state.dialog.title.resolveReference(),
            message = state.dialog.message.resolveReference(),
            confirmButton = DialogButtonUM(
                title = state.dialog.confirmButtonText.resolveReference(),
                onClick = state.dialog.onConfirmClick,
            ),
            dismissButton = DialogButtonUM(
                title = state.dialog.dismissButtonText.resolveReference(),
                warning = state.dialog.dismissWarningColor,
                onClick = state.dialog.onDismissButtonClick,
            ),
            onDismissDialog = state.dialog.onDismiss,
        )
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
            ) {
                Text(
                    text = stringResourceSafe(id = R.string.onboarding_seed_import_message),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(start = 36.dp, end = 36.dp, top = 24.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                )

                PhraseBlock(
                    state = state,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                OutlineTextFieldWithIcon(
                    modifier = modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    value = state.passPhrase,
                    onValueChange = state.passPhraseChange,
                    iconResId = R.drawable.ic_information_24,
                    iconColor = TangemTheme.colors.icon.informative,
                    label = stringResourceSafe(id = R.string.common_passphrase),
                    placeholder = stringResourceSafe(id = R.string.send_optional_field),
                    onIconClick = state.onPassphraseInfoClick,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                    ),
                )
            }

            PrimaryButtonIconEnd(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(id = R.string.common_import),
                iconResId = R.drawable.ic_tangem_24,
                enabled = state.createWalletEnabled,
                showProgress = state.createWalletProgress,
                onClick = state.createWalletClick,
            )
        }

        val keyboard by keyboardAsState()
        if (keyboard is Keyboard.Opened) {
            SuggestionsBlock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
                suggestionsList = state.suggestionsList,
                onClick = { index -> state.onSuggestionClick(state.suggestionsList[index]) },
            )
        }
    }

    PassphraseInfoBottomSheet(state.infoBottomSheetConfig)
}

@Composable
private fun PhraseBlock(state: MultiWalletSeedPhraseUM.Import, modifier: Modifier = Modifier) {
    val warningColor = TangemTheme.colors.text.warning
    val invalidWordsColorTransformation = remember(state.invalidWords, warningColor) {
        InvalidWordsColorTransformation(
            wordsToBrush = state.invalidWords,
            style = SpanStyle(color = warningColor),
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size142),
            value = state.words,
            onValueChange = state.wordsChange,
            textStyle = TangemTheme.typography.body1,
            singleLine = false,
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
            visualTransformation = invalidWordsColorTransformation,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
            ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(TangemTheme.dimens.size32),
        ) {
            if (state.wordsErrorText != null) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    text = state.wordsErrorText.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.warning,
                )
            }
        }
    }
}

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
            modifier = Modifier.padding(bottom = 8.dp),
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
    Notifier(
        text = text,
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCorners8)
            .clickable(onClick = onClick),
        backgroundColor = TangemTheme.colors.icon.primary1,
        textColor = TangemTheme.colors.text.primary2,
    )
}

private fun Modifier.rowPadding(index: Int, rowSize: Int, outSide: Dp, inSide: Dp): Modifier = when (index) {
    0 -> this.padding(start = outSide, end = inSide)
    rowSize - 1 -> this.padding(start = inSide, end = outSide)
    else -> this.padding(horizontal = inSide)
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview(
        alwaysShowBottomSheets = false,
    ) {
        MultiWalletSeedPhraseImport(
            state = MultiWalletSeedPhraseUM.Import(
                wordsErrorText = stringReference("Error"),
            ),
        )
    }
}