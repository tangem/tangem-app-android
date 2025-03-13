package com.tangem.feature.onboarding.presentation.wallet2.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.onboarding.R
import com.tangem.feature.onboarding.presentation.wallet2.model.*
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.DescriptionSubTitleText
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.DescriptionTitleText
import com.tangem.feature.onboarding.presentation.wallet2.ui.components.OnboardingActionBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
[REDACTED_AUTHOR]
 */
@Composable
fun YourSeedPhraseScreen(state: YourSeedPhraseState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing92)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            SegmentSeedBlock(
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing20),
                state = state.segmentSeedState,
            )

            TitleBlock(state.segmentSeedState)

            SeedPhraseGridBlock(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing20)
                    .padding(horizontal = TangemTheme.dimens.size16),
                mnemonicGridItems = state.mnemonicGridItems,
            )
        }
        OnboardingActionBlock(
            modifier = Modifier.align(Alignment.BottomCenter),
            firstActionContent = {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResourceSafe(id = R.string.common_continue),
                    onClick = state.buttonContinue.onClick,
                )
            },
        )
    }
}

@Composable
private fun TitleBlock(state: SegmentSeedState) {
    Box(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.size36)
            .padding(top = TangemTheme.dimens.spacing20)
            .fillMaxWidth(),
    ) {
        Column {
            DescriptionTitleText(text = stringResourceSafe(id = R.string.onboarding_seed_generate_title))
            SpacerH16()
            DescriptionSubTitleText(
                text = pluralStringResourceSafe(
                    id = R.plurals.onboarding_seed_generate_message_words_count,
                    count = state.selectedSeedType.count,
                    state.selectedSeedType.count,
                ),
            )
        }
    }
}

@Composable
private fun SegmentSeedBlock(state: SegmentSeedState, modifier: Modifier = Modifier) {
    SegmentedButtons(
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing76),
        config = state.seedSegments,
        initialSelectedItem = state.selectedSeedType,
        onClick = {
            state.onSelectType(it)
        },
    ) {
        Text(
            text = pluralStringResourceSafe(
                id = R.plurals.onboarding_seed_generate_words_count,
                count = it.count,
                it.count,
            ),
            modifier = Modifier
                .padding(vertical = TangemTheme.dimens.spacing10)
                .fillMaxWidth(),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SeedPhraseGridBlock(mnemonicGridItems: ImmutableList<MnemonicGridItem>, modifier: Modifier = Modifier) {
    VerticalGrid(
        modifier = modifier,
        items = mnemonicGridItems,
        columns = 2,
    ) { item ->
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.size8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

@Composable
private inline fun <T> VerticalGrid(
    items: ImmutableList<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    crossinline content: @Composable (T) -> Unit,
) {
    val columnLength = items.size / columns
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (index in 0 until columns) {
            Column {
                for (i in 0 until columnLength) {
                    val item = items[index * columnLength + i]
                    content(item)
                }
            }
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YourSeedPhraseScreenPreview_Light() {
    TangemThemePreview {
        YourSeedPhraseScreen(
            state = YourSeedPhraseState(
                segmentSeedState = SegmentSeedState(
                    seedSegments = persistentListOf(
                        SegmentSeedType.SEED_12,
                        SegmentSeedType.SEED_24,
                    ),
                    selectedSeedType = SegmentSeedType.SEED_24,
                    onSelectType = {},
                ),
                mnemonicGridItems = (1..24).map { MnemonicGridItem(it, it.toString()) }.toPersistentList(),
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