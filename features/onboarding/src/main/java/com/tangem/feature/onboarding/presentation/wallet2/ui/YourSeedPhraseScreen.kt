package com.tangem.feature.onboarding.presentation.wallet2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerW32
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingDescription
import com.tangem.feature.onboarding.presentation.wallet2.model.YourSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingDescriptionBlock

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun YourSeedPhraseScreen(
    modifier: Modifier = Modifier,
    state: YourSeedPhraseState = YourSeedPhraseState(),
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
                        titleRes = R.string.onboarding_seed_generate_title,
                        subTitleRes = R.string.onboarding_seed_generate_message,
                    ),
                ),
            )
        }
        Box(
            modifier = Modifier.weight(1f),
        ) {
            PhraseGreedBlock(state.phraseList)
        }

        Box(
            modifier = Modifier.wrapContentSize(),
        ) {
            OnboardingActionBlock(
                firstActionContent = {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.size16),
                        text = stringResource(id = R.string.common_continue),
                        onClick = state.buttonContinue.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun PhraseGreedBlock(phraseList: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
    ) {
        this.items(
            count = phraseList.size,
        ) { index ->
            Row(
                modifier = Modifier.padding(all = TangemTheme.dimens.size8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpacerW32()
                Text(
                    modifier = Modifier.width(TangemTheme.dimens.size40),
                    text = "${index + 1}.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                Text(
                    text = phraseList[index],
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
            }

        }
    }
}
