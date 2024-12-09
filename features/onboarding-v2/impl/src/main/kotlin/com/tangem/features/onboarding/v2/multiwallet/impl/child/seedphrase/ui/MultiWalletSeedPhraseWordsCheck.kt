package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.resolveReference
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
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.onboarding_seed_user_validation_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 48.dp, start = 36.dp, end = 36.dp, bottom = 16.dp)
                .fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_seed_user_validation_message),
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

        Box(
            Modifier.weight(1f),
        ) {
            PrimaryButtonIconEnd(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(),
                text = stringResource(id = R.string.onboarding_create_wallet_button_create_wallet),
                iconResId = R.drawable.ic_tangem_24,
                enabled = state.createWalletButtonEnabled,
                showProgress = state.createWalletButtonProgress,
                onClick = state.onCreateWalletButtonClick,
            )
        }
    }
}

@Composable
private fun Fields(state: MultiWalletSeedPhraseUM.GeneratedWordsCheck, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.wordFields.forEach { field ->
            OutlineTextField(
                value = field.word,
                onValueChange = field.onChange,
                label = field.index.toString(),
                isError = field.error,
            )
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