package com.tangem.features.hotwallet.viewphrase.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.grid.EnumeratedTwoColumnGrid
import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.viewphrase.entity.ViewPhraseUM
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ViewPhraseContent(state: ViewPhraseUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        AppBarWithBackButton(
            text = stringResourceSafe(R.string.common_backup),
            onBackClick = state.onBackClick,
        )

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

            EnumeratedTwoColumnGrid(
                items = state.words,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun TitleBlock(state: ViewPhraseUM, modifier: Modifier = Modifier) {
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
                R.string.backup_seed_caution,
                state.words.size,
            ),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        ViewPhraseContent(
            state = ViewPhraseUM(
                onBackClick = {},
                words = List(12) {
                    EnumeratedTwoColumnGridItem(
                        index = it + 1,
                        mnemonic = "word${it + 1}",
                    )
                }.toImmutableList(),
            ),
        )
    }
}