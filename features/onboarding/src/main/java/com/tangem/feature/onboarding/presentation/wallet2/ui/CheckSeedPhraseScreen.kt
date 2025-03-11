package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.R
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.fields.contextmenu.DisableContextMenu
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.Description
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
[REDACTED_AUTHOR]
 */
@Suppress("ReusedModifierInstance")
@Composable
fun CheckSeedPhraseScreen(state: CheckSeedPhraseState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            item {
                OnboardingDescriptionBlock(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = TangemTheme.dimens.size48)
                        .padding(horizontal = TangemTheme.dimens.size16),
                ) {
                    Description(
                        titleRes = R.string.onboarding_seed_user_validation_title,
                        subTitleRes = R.string.onboarding_seed_user_validation_message,
                    )
                }
            }
            item {
                CheckSeedPhraseBlock(
                    modifier = Modifier
                        .imePadding()
                        .weight(1f)
                        .padding(top = TangemTheme.dimens.size20)
                        .padding(horizontal = TangemTheme.dimens.size16),
                    state = state,
                )
            }
        }
        OnboardingActionBlock(
            firstActionContent = {
                PrimaryButtonIconEnd(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResourceSafe(id = R.string.onboarding_create_wallet_button_create_wallet),
                    iconResId = R.drawable.ic_tangem_24,
                    enabled = state.buttonCreateWallet.enabled,
                    showProgress = state.buttonCreateWallet.showProgress,
                    onClick = state.buttonCreateWallet.onClick,
                )
            },
        )
    }
}

@Composable
private fun CheckSeedPhraseBlock(state: CheckSeedPhraseState, modifier: Modifier = Modifier) {
    Column(modifier) {
        CheckSeedPhraseTextField(state = state.tvSecondPhrase, imeAction = ImeAction.Next)
        CheckSeedPhraseTextField(state = state.tvSeventhPhrase, imeAction = ImeAction.Next)
        CheckSeedPhraseTextField(state = state.tvEleventhPhrase, imeAction = ImeAction.Done)
    }
}

@Composable
private fun CheckSeedPhraseTextField(state: TextFieldState, imeAction: ImeAction) {
    val focusManager = LocalFocusManager.current

    DisableContextMenu {
        OutlineTextField(
            value = state.textFieldValue,
            onValueChange = state.onTextFieldValueChanged,
            label = state.label,
            isError = state.isError,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
                onNext = { focusManager.moveFocus(focusDirection = FocusDirection.Down) },
            ),
        )
    }
}