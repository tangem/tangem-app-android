package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.R
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.fields.contextmenu.DisableContextMenu
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun MultiWalletSeedPhraseWordsCheck(
    state: MultiWalletSeedPhraseUM.GeneratedWordsCheck,
    modifier: Modifier = Modifier,
) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
        ) {
            Text(
                text = stringResourceSafe(R.string.onboarding_seed_user_validation_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 48.dp, start = 36.dp, end = 36.dp, bottom = 16.dp)
                    .fillMaxWidth(),
            )
            Text(
                text = stringResourceSafe(R.string.onboarding_seed_user_validation_message),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .fillMaxWidth(),
            )

            Fields(
                state = state,
                modifier = Modifier
                    .padding(vertical = 30.dp, horizontal = 16.dp),
            )
        }

        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(16.dp)
                .imePadding()
                .fillMaxWidth(),
            text = stringResourceSafe(id = R.string.onboarding_create_wallet_button_create_wallet),
            iconResId = R.drawable.ic_tangem_24,
            enabled = state.createWalletButtonEnabled,
            showProgress = state.createWalletButtonProgress,
            onClick = state.onCreateWalletButtonClick,
        )
    }
}

@Composable
private fun Fields(state: MultiWalletSeedPhraseUM.GeneratedWordsCheck, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DisableContextMenu {
            state.wordFields.fastForEachIndexed { index, field ->
                OutlineTextField(
                    value = field.word,
                    onValueChange = field.onChange,
                    label = field.index.toString(),
                    isError = field.error,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = if (index == state.wordFields.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                        onNext = { focusManager.moveFocus(focusDirection = FocusDirection.Down) },
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletSeedPhraseWordsCheck(
            state = MultiWalletSeedPhraseUM.GeneratedWordsCheck(
                wordFields = persistentListOf(
                    MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField(
                        index = 2,
                        word = TextFieldValue(text = "word"),
                        onChange = {},
                        error = false,
                    ),
                    MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField(
                        index = 7,
                        word = TextFieldValue(text = "wor"),
                        onChange = {},
                        error = true,
                    ),
                    MultiWalletSeedPhraseUM.GeneratedWordsCheck.WordField(
                        index = 11,
                        word = TextFieldValue(text = "word"),
                        onChange = {},
                        error = false,
                    ),
                ),
            ),
        )
    }
}
