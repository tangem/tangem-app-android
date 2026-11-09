package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeysUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingSettingsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PolymarketFastTradingSettingsScreen(
    state: PolymarketFastTradingSettingsUM,
    modifier: Modifier = Modifier,
) {
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TangemTopNavigation(
                    title = resourceReference(R.string.prediction_session_key_settings_title),
                    contentAlign = TangemTopNavigation.ContentAlign.Center,
                    onBack = state.onBackClick,
                )
            },
        ) { contentPadding ->
            when (state) {
                is PolymarketFastTradingSettingsUM.Loading -> KeysSkeleton(contentPadding)
                is PolymarketFastTradingSettingsUM.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    PolymarketReloadPrompt(
                        text = resourceReference(R.string.common_something_went_wrong),
                        onReloadClick = state.onRetryClick,
                    )
                }
                is PolymarketFastTradingSettingsUM.Content -> TangemPullToRefreshSlidingContainer(
                    config = state.pullToRefreshConfig,
                    indicatorOffset = contentPadding.calculateTopPadding(),
                ) {
                    KeysList(keys = state.keys, onActionClick = state.onActionClick, contentPadding = contentPadding)
                }
            }
        }
    }
}

@Composable
private fun KeysList(
    keys: FastTradingKeysUM,
    onActionClick: (FastTradingKeyActionUM) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        unknownSignersSection(keys, onActionClick)
        keys.thisDevice?.let { row ->
            sectionHeader(key = "this_device_header", title = R.string.prediction_session_key_this_device)
            item(key = "this_device:${row.id}", contentType = "key") {
                PolymarketFastTradingKeyRow(
                    state = row,
                    areActionsEnabled = keys.areActionsEnabled,
                    onActionClick = onActionClick,
                )
            }
        }
        if (keys.otherDevices.isNotEmpty()) {
            sectionHeader(key = "other_devices_header", title = R.string.prediction_session_key_other_devices)
            items(keys.otherDevices, key = { "other_device:${it.id}" }, contentType = { "key" }) { row ->
                PolymarketFastTradingKeyRow(
                    state = row,
                    areActionsEnabled = keys.areActionsEnabled,
                    onActionClick = onActionClick,
                )
            }
        }
    }
}

private fun LazyListScope.unknownSignersSection(
    keys: FastTradingKeysUM,
    onActionClick: (FastTradingKeyActionUM) -> Unit,
) {
    if (!keys.isUnknownSignersWarningShown) return
    item(key = "unknown_signers_banner", contentType = "banner") {
        TangemMessageBanner(
            title = resourceReference(R.string.prediction_session_key_unknown_signers_title),
            description = resourceReference(R.string.prediction_session_key_unknown_signers_text),
            variant = TangemMessageBanner.Variant.Warning,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    items(keys.unknownSigners, key = { "unknown_signer:${it.address}" }, contentType = { "signer" }) { signer ->
        PolymarketFastTradingUnknownSignerRow(
            state = signer,
            areActionsEnabled = keys.areActionsEnabled,
            onActionClick = onActionClick,
        )
    }
}

private fun LazyListScope.sectionHeader(key: String, title: Int) {
    item(key = key, contentType = "header") {
        TangemRow(titleSlot = { TangemRowText(text = resourceReference(title), role = TangemRowTextRole.Subtitle) })
    }
}

@Composable
private fun KeysSkeleton(contentPadding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        repeat(SKELETON_ROWS) {
            TangemRow(
                startSlot = { TangemAvatar(state = TangemAvatar.UiState.Shimmer(), size = TangemAvatar.Size.X40) },
                titleSlot = { TangemShimmer(modifier = Modifier.width(120.dp).height(16.dp)) },
                subtitleSlot = { TangemShimmer(modifier = Modifier.width(80.dp).height(14.dp)) },
            )
        }
    }
}

private const val SKELETON_ROWS = 2

@Preview(name = "Light", widthDp = 390, heightDp = 800)
@Preview(name = "Dark", widthDp = 390, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingSettingsScreenPreview(
    @PreviewParameter(SettingsPreviewProvider::class) state: PolymarketFastTradingSettingsUM,
) {
    TangemThemePreviewRedesign {
        PolymarketFastTradingSettingsScreen(state = state)
    }
}

private class SettingsPreviewProvider : PreviewParameterProvider<PolymarketFastTradingSettingsUM> {
    override val values = sequenceOf(
        PolymarketFastTradingSettingsUM.Content(
            onBackClick = {},
            keys = FastTradingKeysUM(
                isUnknownSignersWarningShown = true,
                unknownSigners = persistentListOf(
                    FastTradingUnknownSignerUM(
                        address = PREVIEW_SIGNER_ADDRESS,
                        actions = persistentListOf(FastTradingKeyActionUM.Revoke(PREVIEW_SIGNER_ADDRESS)),
                    ),
                ),
                thisDevice = previewRow("Pixel 9", FastTradingKeyStatusUM.Active("20 Apr 2027"), isExpiringSoon = true),
                otherDevices = persistentListOf(
                    previewRow("Galaxy S24", FastTradingKeyStatusUM.Paused("2 May 2027")),
                    previewRow("iPhone 16", FastTradingKeyStatusUM.Revoking),
                    previewRow("Pixel 7", FastTradingKeyStatusUM.Expired("1 Oct 2026")),
                    previewRow("Pixel 8", FastTradingKeyStatusUM.ActivationFailed),
                    previewRow("iPad", FastTradingKeyStatusUM.ReconnectNeeded),
                ),
                areActionsEnabled = true,
            ),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            onActionClick = {},
        ),
        PolymarketFastTradingSettingsUM.Loading(onBackClick = {}),
        PolymarketFastTradingSettingsUM.Error(onBackClick = {}, onRetryClick = {}),
    )

    private fun previewRow(name: String, status: FastTradingKeyStatusUM, isExpiringSoon: Boolean = false) =
        FastTradingKeyRowUM(
            id = name,
            deviceName = stringReference(name),
            status = status,
            isExpiringSoon = isExpiringSoon,
            actions = if (status == FastTradingKeyStatusUM.Revoking) {
                persistentListOf()
            } else {
                persistentListOf(FastTradingKeyActionUM.Revoke(name))
            },
        )
}

private const val PREVIEW_SIGNER_ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"