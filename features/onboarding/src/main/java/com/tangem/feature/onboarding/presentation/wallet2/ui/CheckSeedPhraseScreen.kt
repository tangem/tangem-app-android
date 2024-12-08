package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseState
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
        OutlineTextField(
            value = state.tvSecondPhrase.textFieldValue,
            onValueChange = state.tvSecondPhrase.onTextFieldValueChanged,
            label = state.tvSecondPhrase.label,
            isError = state.tvSecondPhrase.isError,
        )

        OutlineTextField(
            value = state.tvSeventhPhrase.textFieldValue,
            onValueChange = state.tvSeventhPhrase.onTextFieldValueChanged,
            label = state.tvSeventhPhrase.label,
            isError = state.tvSeventhPhrase.isError,
        )

        OutlineTextField(
            value = state.tvEleventhPhrase.textFieldValue,
            onValueChange = state.tvEleventhPhrase.onTextFieldValueChanged,
            label = state.tvEleventhPhrase.label,
            isError = state.tvEleventhPhrase.isError,
        )
    }
}