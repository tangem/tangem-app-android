package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconLeft
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun ImportSeedPhraseScreen(
    modifier: Modifier = Modifier,
    state: ImportSeedPhraseState = ImportSeedPhraseState(),
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(0.6f),
        ) {
            OnboardingDescriptionBlock(
                modifier = Modifier.align(Alignment.Center),
                descriptionsList = listOf(
                    OnboardingDescription(
                        subTitle = "To import your wallet, enter your secret recovery phrase in the fields below or scan a QR-code",
                    ),
                ),
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.size16)
                .weight(1f),
        ) {
            PhraseBlock(state)
        }

        Box(
            modifier = Modifier.weight(1f),
        ) {
            SuggestionsBlock(state)
        }

        Box(
            modifier = Modifier.wrapContentSize(),
        ) {
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
}

@Composable
private fun PhraseBlock(state: ImportSeedPhraseState) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size142),
        // value = TextFieldValue(text = state.tvPhrase.text),
        value = TextFieldValue(text = "state.tvPhrase.text"),
        onValueChange = {  },
        // textStyle = TangemTheme.typography.body1,
        // singleLine = false,
    )
}

@Composable
private fun SuggestionsBlock(state: ImportSeedPhraseState) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.suggestionsList.size) { index ->
            PrimaryButton(
                modifier = Modifier
                    .padding(all = TangemTheme.dimens.size4)
                    .background(color = TangemTheme.colors.background.action),
                text = state.suggestionsList[index],
                onClick = { state.onSuggestedPhraseClick(index) },
            )
        }
    }
}

@Composable
private fun ReadMoreBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        val shape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(shape)
                .border(1.dp, Color.Gray, shape)
                .clickable(
                    onClick = {},
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_top_right),
                contentDescription = "Go away icon",
            )
            SpacerW8()
            Text(
                text = stringResource(id = R.string.onboarding_seed_button_read_more),
            )
        }
    }
}
