package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.GeneratedWordsType
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun MultiWalletSeedPhraseWords(
    state: MultiWalletSeedPhraseUM.GenerateSeedPhrase,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .weight(1f),
        ) {
            SegmentSeedBlock(
                modifier = Modifier.padding(vertical = 20.dp),
                state = state,
            )

            TitleBlock(state)

            SeedPhraseGridBlock(
                mnemonicGridItems = state.words,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 32.dp),
            )
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            text = stringResourceSafe(id = R.string.common_continue),
            onClick = state.onContinueClick,
        )
    }
}

@Suppress("ComplexCondition", "MagicNumber")
@Composable
private fun SegmentSeedBlock(state: MultiWalletSeedPhraseUM.GenerateSeedPhrase, modifier: Modifier = Modifier) {
    SegmentedButtons(
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing76),
        config = persistentListOf(GeneratedWordsType.Words12, GeneratedWordsType.Words24),
        initialSelectedItem = state.option,
        onClick = {
            when {
                state.option == GeneratedWordsType.Words12 && it == GeneratedWordsType.Words24 -> {
                    state.onOptionChange(it)
                }
                state.option == GeneratedWordsType.Words24 && it == GeneratedWordsType.Words12 -> {
                    state.onOptionChange(it)
                }
            }
        },
    ) {
        Text(
            text = pluralStringResourceSafe(
                id = R.plurals.onboarding_seed_generate_words_count,
                count = it.length,
                it.length,
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

@Suppress("MagicNumber")
@Composable
private fun TitleBlock(state: MultiWalletSeedPhraseUM.GenerateSeedPhrase, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.size36)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceSafe(id = R.string.onboarding_seed_generate_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = pluralStringResourceSafe(
                id = R.plurals.onboarding_seed_generate_message_words_count,
                count = state.option.length,
                state.option.length,
            ),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SeedPhraseGridBlock(mnemonicGridItems: ImmutableList<MnemonicGridItem>, modifier: Modifier = Modifier) {
    VerticalGrid(
        modifier = modifier,
        items = mnemonicGridItems,
    ) { item ->
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.size8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
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
            } else {
                Text(
                    text = item.mnemonic,
                    style = TangemTheme.typography.button,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    modifier = Modifier.width(TangemTheme.dimens.size40),
                    text = "${item.index}.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private inline fun <T> VerticalGrid(
    items: ImmutableList<T>,
    modifier: Modifier = Modifier,
    crossinline content: @Composable (T) -> Unit,
) {
    val columnLength = items.size / 2
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(2) { index ->
            Column {
                for (i in 0 until columnLength) {
                    val item = items[index * columnLength + i]
                    content(item)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun Preview() {
    TangemThemePreview {
        MultiWalletSeedPhraseWords(
            state = MultiWalletSeedPhraseUM.GenerateSeedPhrase(
                words = List(24) {
                    MnemonicGridItem(
                        index = it + 1,
                        mnemonic = "word1",
                    )
                }.toImmutableList(),
            ),
        )
    }
}