package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerW32
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.ButtonState
import com.tangem.feature.onboarding.presentation.wallet2.model.MnemonicGridItem
import com.tangem.feature.onboarding.presentation.wallet2.model.YourSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.Description
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
[REDACTED_AUTHOR]
 */
@Composable
fun YourSeedPhraseScreen(state: YourSeedPhraseState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.weight(weight = 0.6f),
        ) {
            OnboardingDescriptionBlock(
                modifier = Modifier.align(Alignment.Center),
            ) {
                Description(
                    titleRes = R.string.onboarding_seed_generate_title,
                    subTitleRes = R.string.onboarding_seed_generate_message,
                )
            }
        }
        Box(
            modifier = Modifier.weight(weight = 1f),
        ) {
            PhraseGreedBlock(state.mnemonicGridItems)
        }

        Box {
            OnboardingActionBlock(
                firstActionContent = {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.common_continue),
                        onClick = state.buttonContinue.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun PhraseGreedBlock(mnemonicGridItems: ImmutableList<MnemonicGridItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(count = 2),
    ) {
        this.items(
            count = mnemonicGridItems.size,
        ) { index ->
            Row(
                modifier = Modifier.padding(all = TangemTheme.dimens.size8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val item = mnemonicGridItems[index]
                SpacerW32()
                Text(
                    modifier = Modifier.width(TangemTheme.dimens.size40),
                    text = "${item.index}.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                Text(
                    text = item.mnemonic,
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

@Preview
@Composable
private fun YourSeedPhraseScreenPreview_Light() {
    TangemTheme(isDark = false) {
        YourSeedPhraseScreen(
            state = YourSeedPhraseState(
                mnemonicGridItems = (1..12).map { MnemonicGridItem(it, it.toString()) }.toImmutableList(),
                buttonContinue = ButtonState(
                    enabled = true,
                    isClickable = true,
                    showProgress = false,
                    onClick = {},
                ),
            ),
            modifier = Modifier
                .fillMaxSize()
                .background(color = TangemTheme.colors.background.primary),
        )
    }
}