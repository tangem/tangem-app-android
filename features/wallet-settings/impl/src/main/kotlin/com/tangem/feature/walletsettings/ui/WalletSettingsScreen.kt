package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.appbar.models.TopAppBarMedium
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.BlockItem
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.walletsettings.component.preview.PreviewWalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletSettingsScreen(
    state: WalletSettingsUM,
    dialog: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = TangemTheme.colors.background.secondary
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(onBack = state.popBack)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = backgroundColor,
        snackbarHost = {
            TangemSnackbarHost(
                modifier = Modifier.padding(all = TangemTheme.dimens.spacing16),
                hostState = LocalSnackbarHostState.current,
            )
        },
        topBar = {
            TopAppBarMedium(
                title = resourceReference(R.string.wallet_settings_title),
                scrollBehavior = scrollBehavior,
                onBackClick = state.popBack,
            )
        },
        content = { paddingValues ->
            Content(
                modifier = Modifier.padding(paddingValues),
                state = state,
            )

            dialog()
        },
    )
}

@Composable
private fun Content(state: WalletSettingsUM, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        contentPadding = PaddingValues(
            top = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        items(
            items = state.items,
            key = WalletSettingsItemUM::id,
        ) { item ->
            val itemModifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)

            when (item) {
                is WalletSettingsItemUM.WithItems -> ItemsBlock(
                    modifier = itemModifier,
                    model = item,
                )
                is WalletSettingsItemUM.WithText -> TextBlock(
                    modifier = itemModifier,
                    model = item,
                )
            }
        }
    }
}

@Composable
private fun ItemsBlock(model: WalletSettingsItemUM.WithItems, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                    color = TangemTheme.colors.background.primary,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            model.blocks.forEach { block ->
                BlockItem(model = block)
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            text = model.description.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Composable
private fun TextBlock(model: WalletSettingsItemUM.WithText, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier.fillMaxWidth(),
        onClick = model.onClick,
    ) {
        Column(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            Text(
                text = model.title.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = model.text.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.body1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_WalletSettingsScreen() {
    TangemThemePreview {
        PreviewWalletSettingsComponent().Content(modifier = Modifier.fillMaxSize())
    }
}
// endregion Preview