package com.tangem.feature.tester.presentation.excludedblockchains

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomFade
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.rows.RowContentContainer
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.excludedblockchains.state.BlockchainUM
import com.tangem.feature.tester.presentation.excludedblockchains.state.ExcludedBlockchainsScreenUM
import com.tangem.feature.tester.presentation.excludedblockchains.state.mapper.toUiModels
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExcludedBlockchainsScreen(state: ExcludedBlockchainsScreenUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.primary,
        topBar = {
            TangemTopAppBar(
                title = resourceReference(R.string.excluded_blockchains),
                startButton = TopAppBarButtonUM.Back(onBackClicked = state.popBack),
                endButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_refresh_24,
                    onClicked = state.onRecoverClick,
                ),
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = "Restart App",
                onClick = state.onRestartClick,
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .bottomFade()
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 96.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            ) {
                item {
                    SearchBar(
                        modifier = Modifier.padding(bottom = 12.dp),
                        state = state.search,
                    )
                }

                item {
                    TogglesNotification(
                        modifier = Modifier.padding(vertical = 12.dp),
                        appVersion = state.appVersion,
                        isVisible = state.showRecoverWarning,
                    )
                }

                items(
                    items = state.blockchains,
                    key = BlockchainUM::id,
                ) { item ->
                    BlockchainItem(
                        model = item,
                    )
                }
            }
        },
    )
}

@Composable
private fun TogglesNotification(appVersion: String, isVisible: Boolean, modifier: Modifier = Modifier) {
    if (isVisible) {
        Notification(
            modifier = modifier,
            config = NotificationConfig(
                subtitle = resourceReference(
                    id = R.string.feature_toggles_custom_setup_warning_description,
                    wrappedList(appVersion),
                ),
                iconResId = R.drawable.ic_alert_triangle_20,
                title = resourceReference(id = R.string.custom_setup_warning_title),
            ),
        )
    }
}

@Composable
private fun BlockchainItem(model: BlockchainUM, modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size52),
        icon = {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TangemTheme.dimens.size22),
                painter = painterResource(id = model.iconResId),
                contentDescription = null,
            )
        },
        text = {
            RowText(
                mainText = model.name,
                secondText = model.symbol,
                accentMainText = false,
                accentSecondText = false,
            )
        },
        action = {
            TangemSwitch(
                checked = model.isExcluded,
                onCheckedChange = model.onExcludedStateChange,
            )
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_ExcludedBlockchainsScreen(
    @PreviewParameter(ExcludedBlockchainsScreenPreviewProvider::class) params: ExcludedBlockchainsScreenUM,
) {
    TangemThemePreview {
        ExcludedBlockchainsScreen(params)
    }
}

private class ExcludedBlockchainsScreenPreviewProvider : PreviewParameterProvider<ExcludedBlockchainsScreenUM> {
    override val values: Sequence<ExcludedBlockchainsScreenUM>
        get() = sequenceOf(
            ExcludedBlockchainsScreenUM(
                blockchains = Blockchain.entries.toUiModels(
                    excludedBlockchainsIds = setOf(Blockchain.Ethereum.id, Blockchain.Solana.id),
                    onExcludedStateChange = { _, _ -> },
                ).toPersistentList(),
                showRecoverWarning = true,
                appVersion = "1.0.0",
                search = SearchBarUM(
                    placeholderText = resourceReference(R.string.excluded_blockchains_search_placeholder),
                    query = "",
                    isActive = false,
                    onQueryChange = {},
                    onActiveChange = {},
                ),
                popBack = {},
                onRecoverClick = {},
                onRestartClick = {},
            ),
        )
}
// endregion Preview