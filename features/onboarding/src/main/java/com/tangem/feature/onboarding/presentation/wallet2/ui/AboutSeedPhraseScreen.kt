package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.AboutState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.Description
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
[REDACTED_AUTHOR]
 */
@Composable
fun AboutSeedPhraseScreen(state: AboutState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(weight = 0.8f),
        ) {
            EditIconBlock()
        }
        Box(
            modifier = Modifier.weight(weight = 1.2f),
        ) {
            Column {
                OnboardingDescriptionBlock {
                    Description(
                        titleRes = R.string.onboarding_seed_intro_title,
                        subTitleRes = R.string.onboarding_seed_intro_message,
                    )
                }
                ReadMoreBlock(state)
            }
        }
        Box {
            OnboardingActionBlock(
                firstActionContent = {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResourceSafe(id = R.string.onboarding_seed_intro_button_generate),
                        enabled = state.buttonGenerateSeedPhrase.enabled,
                        showProgress = state.buttonGenerateSeedPhrase.showProgress,
                        onClick = state.buttonGenerateSeedPhrase.onClick,
                    )
                },
                secondActionContent = {
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResourceSafe(id = R.string.onboarding_seed_intro_button_import),
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
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            modifier = Modifier
                .size(TangemTheme.dimens.size44),
            painter = painterResource(id = R.drawable.ic_onboarding_text_edit_56),
            contentDescription = null,
        )
        SpacerH32()
        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.icon.warning.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(TangemTheme.dimens.radius8),
                ),
        ) {
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = TangemTheme.dimens.size12,
                        vertical = TangemTheme.dimens.size4,
                    ),
                text = stringResourceSafe(id = R.string.onboarding_seed_phrase_intro_legacy),
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
            .padding(top = TangemTheme.dimens.size16),
    ) {
        val shape = TangemTheme.shapes.roundedCornersLarge
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(shape)
                .border(
                    width = TangemTheme.dimens.size1,
                    color = TangemTheme.colors.stroke.primary,
                    shape = shape,
                )
                .clickable(onClick = state.buttonReadMoreAboutSeedPhrase.onClick)
                .padding(
                    horizontal = TangemTheme.dimens.size16,
                    vertical = TangemTheme.dimens.size8,
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary1,
            )
            SpacerW8()
            Text(
                text = stringResourceSafe(id = R.string.onboarding_seed_button_read_more),
                color = TangemTheme.colors.text.primary1,
            )
        }
    }
}