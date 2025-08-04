package com.tangem.features.hotwallet.manualbackup.check.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.fields.contextmenu.DisableContextMenu
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.manualbackup.check.entity.ManualBackupCheckUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ManualBackupCheckContent(state: ManualBackupCheckUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
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

        PrimaryButton(
            modifier = Modifier
                .padding(16.dp)
                .imePadding()
                .fillMaxWidth(),
            text = stringResourceSafe(id = R.string.common_continue),
            enabled = state.completeButtonEnabled,
            showProgress = state.completeButtonProgress,
            onClick = state.onCompleteButtonClick,
        )
    }
}

@Composable
private fun Fields(state: ManualBackupCheckUM, modifier: Modifier = Modifier) {
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
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        ManualBackupCheckContent(
            state = ManualBackupCheckUM(
                onCompleteButtonClick = {},
                wordFields = persistentListOf(
                    ManualBackupCheckUM.WordField(
                        index = 2,
                        word = TextFieldValue(text = "word"),
                        onChange = {},
                        error = false,
                    ),
                    ManualBackupCheckUM.WordField(
                        index = 7,
                        word = TextFieldValue(text = "wor"),
                        onChange = {},
                        error = true,
                    ),
                    ManualBackupCheckUM.WordField(
                        index = 11,
                        word = TextFieldValue(text = "word"),
                        onChange = {},
                        error = false,
                    ),
                ),
                completeButtonEnabled = false,
                completeButtonProgress = false,
            ),
        )
    }
}