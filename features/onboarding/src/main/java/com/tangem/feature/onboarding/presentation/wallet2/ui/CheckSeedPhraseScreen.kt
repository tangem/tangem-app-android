package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButtonIconLeft
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
[REDACTED_AUTHOR]
 */
@Composable
fun CheckSeedPhraseScreen(
    modifier: Modifier = Modifier,
    state: CheckSeedPhraseState = CheckSeedPhraseState(),
) {
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
                        .padding(
                            start = TangemTheme.dimens.size16,
                            top = TangemTheme.dimens.size48,
                            end = TangemTheme.dimens.size16,
                        ),
                    descriptionsList = listOf(
                        OnboardingDescription(
                            // FIXME: replace by string res
                            title = "So, let’s check",
                            subTitle = "To check wether you’ve written down your seed phrase correctly, please enter the 2nd, 7th and 11th words",
                        ),
                    ),
                )
            }
            item {
                CheckSeedPhraseBlock(
                    modifier = Modifier
                        .imePadding()
                        .weight(1f)
                        .padding(
                            start = TangemTheme.dimens.size16,
                            top = TangemTheme.dimens.size20,
                            end = TangemTheme.dimens.size16,
                        ),
                    state = state,
                )
            }
        }
        OnboardingActionBlock(
            firstActionContent = {
                PrimaryButtonIconLeft(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TangemTheme.dimens.size16),
                    text = stringResource(id = R.string.onboarding_create_wallet_button_create_wallet),
                    icon = painterResource(id = R.drawable.ic_tangem_24),
                    enabled = state.buttonCreateWallet.enabled,
                    showProgress = state.buttonCreateWallet.showProgress,
                    onClick = state.buttonCreateWallet.onClick,
                )
            },
        )
    }
}

@Composable
private fun CheckSeedPhraseBlock(
    modifier: Modifier = Modifier,
    state: CheckSeedPhraseState,
) {
    Column(modifier) {
        OutlineTextField(
            value = state.tvSecondPhrase.textFieldValue,
            onValueChange = state.tvSecondPhrase.onTextFieldValueChanged,
            label = state.tvSecondPhrase.getLabel(),
            isError = state.tvSecondPhrase.isError,
        )

        OutlineTextField(
            value = state.tvSeventhPhrase.textFieldValue,
            onValueChange = state.tvSeventhPhrase.onTextFieldValueChanged,
            label = state.tvSeventhPhrase.getLabel(),
            isError = state.tvSeventhPhrase.isError,
        )

        OutlineTextField(
            value = state.tvEleventhPhrase.textFieldValue,
            onValueChange = state.tvEleventhPhrase.onTextFieldValueChanged,
            label = state.tvEleventhPhrase.getLabel(),
            isError = state.tvEleventhPhrase.isError,
        )
    }
}

@Composable
private fun CheckSeedPhraseBlock2(
    modifier: Modifier = Modifier,
    state: CheckSeedPhraseState,
) {
    Column(modifier) {
        listOf(state.tvSecondPhrase, state.tvSeventhPhrase, state.tvEleventhPhrase).forEach { field ->
            OutlineTextField(
                value = field.textFieldValue,
                onValueChange = field.onTextFieldValueChanged,
                label = field.getLabel(),
                isError = field.isError,
            )
        }
    }
}

@Composable
private fun TextFieldState.getLabel(): String? {
    return labelRes?.let { stringResource(id = it) } ?: label
}