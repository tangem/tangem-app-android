package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.AboutState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun AboutSeedPhraseScreen(
    modifier: Modifier = Modifier,
    state: AboutState = AboutState(),
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(0.8f),
        ) {
            EditIconBlock()
        }
        Box(
            modifier = Modifier.weight(1.2f),
        ) {
            Column {
                OnboardingDescriptionBlock(
                    descriptionsList = listOf(
                        OnboardingDescription(
                            titleRes = R.string.onboarding_seed_intro_title,
                            subTitleRes = R.string.onboarding_seed_intro_message,
                        ),
                    ),
                )
                ReadMoreBlock(state)
            }
        }

        Box(
            modifier = Modifier.wrapContentSize(),
        ) {
            OnboardingActionBlock(
                firstActionContent = {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.size16),
                        text = stringResource(id = R.string.onboarding_seed_intro_button_generate),
                        enabled = state.buttonGenerateSeedPhrase.enabled,
                        showProgress = state.buttonGenerateSeedPhrase.showProgress,
                        onClick = state.buttonGenerateSeedPhrase.onClick,
                    )
                },
                secondActionContent = {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.size16),
                        text = stringResource(id = R.string.onboarding_seed_intro_button_import),
                        enabled = state.buttonImportSeedPhrase.enabled,
                        showProgress = state.buttonImportSeedPhrase.showProgress,
                        onClick = state.buttonImportSeedPhrase.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun EditIconBlock() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            modifier = Modifier
                .size(44.dp),
            painter = painterResource(id = R.drawable.ic_onboarding_text_edit),
            contentDescription = "Edit icon",
        )
        SpacerH32()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1EE10101)),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                text = stringResource(id = R.string.onboarding_seed_phrase_intro_legacy),
                color = TangemTheme.colors.text.warning,
                style = TangemTheme.typography.body1,
            )
        }

    }
}

@Composable
private fun ReadMoreBlock(state: AboutState) {
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
                .clickable(onClick = state.buttonReadMoreAboutSeedPhrase.onClick)
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
