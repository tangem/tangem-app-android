package com.tangem.features.hotwallet.manualbackup.phrase.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.manualbackup.phrase.entity.ManualBackupPhraseUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ManualBackupPhraseContent(state: ManualBackupPhraseUM, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .weight(1f),
        ) {
            TitleBlock(
                state = state,
                modifier = Modifier.padding(top = 20.dp),
            )

            SeedPhraseGridBlock(
                mnemonicGridItems = state.words,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 32.dp),
            )
        }

        Text(
            text = stringResourceSafe(R.string.backup_seed_responsibility),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            text = stringResourceSafe(id = R.string.common_continue),
            onClick = state.onContinueClick,
        )
    }
}

@Composable
private fun TitleBlock(state: ManualBackupPhraseUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.size36)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.backup_seed_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResourceSafe(
                R.string.backup_seed_description,
                state.words.size,
            ),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SeedPhraseGridBlock(
    mnemonicGridItems: ImmutableList<ManualBackupPhraseUM.MnemonicGridItem>,
    modifier: Modifier = Modifier,
) {
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        ManualBackupPhraseContent(
            state = ManualBackupPhraseUM(
                onContinueClick = {},
                words = List(12) {
                    ManualBackupPhraseUM.MnemonicGridItem(
                        index = it + 1,
                        mnemonic = "word${it + 1}",
                    )
                }.toImmutableList(),
            ),
        )
    }
}