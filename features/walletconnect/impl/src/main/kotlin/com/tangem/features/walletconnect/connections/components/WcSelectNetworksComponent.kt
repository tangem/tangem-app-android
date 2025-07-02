package com.tangem.features.walletconnect.connections.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.features.walletconnect.connections.model.WcSelectNetworksModel
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class WcSelectNetworksComponent(
    appComponentContext: AppComponentContext,
    private val params: WcSelectNetworksParams,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: WcSelectNetworksModel = getOrCreateModel(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        WcSelectNetworksBS(state = state, onBack = router::pop, onDismiss = ::dismiss)
    }

    interface ModelCallback {
        fun onNetworksSelected(selectedNetworks: Set<Network.RawID>)
    }

    data class WcSelectNetworksParams(
        val missingRequiredNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val notAddedNetworks: Set<Network>,
        val enabledAvailableNetworks: Set<Network.RawID>,
        val onDismiss: () -> Unit,
        val callback: ModelCallback,
    )
}

@Composable
private fun WcSelectNetworksBS(
    state: WcSelectNetworksUM,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.required.isEmpty()) return // required can/must not be empty

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = stringReference("Choose networks"),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBack,
            )
        },
        content = {
            WcSelectNetworksContent(
                modifier = modifier.padding(horizontal = 16.dp),
                state = state,
            )
        },
        footer = {
            PrimaryButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                text = "Done",
                onClick = state.onDone,
                enabled = state.missing.isEmpty(),
            )
        },
    )
}

@Composable
private fun WcSelectNetworksContent(state: WcSelectNetworksUM, modifier: Modifier = Modifier) {
    val blocksModifier = Modifier
        .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
        .background(color = TangemTheme.colors.background.action)
        .fillMaxWidth()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.missing.isNotEmpty()) {
            MissingRequiredBlock(modifier = blocksModifier, missingNetworks = state.missing)
        }
        if (state.required.isNotEmpty() || state.available.isNotEmpty()) {
            AvailableNetworksBlock(modifier = blocksModifier, required = state.required, available = state.available)
        }
        if (state.notAdded.isNotEmpty()) {
            NotAddedBlock(modifier = blocksModifier, notAdded = state.notAdded)
        }
    }
}

@Composable
private fun MissingRequiredBlock(missingNetworks: ImmutableList<WcNetworkInfoItem>, modifier: Modifier = Modifier) {
    val missingNetworksName = missingNetworks.joinToString { it.name }
    val notificationUM = remember(missingNetworks) {
        NotificationUM.Info(
            title = stringReference("The wallet has no required networks"),
            subtitle = stringReference("Add the $missingNetworksName network to your portfolio for this wallet."),
        )
    }
    Column(modifier = modifier) {
        Notification(
            config = notificationUM.config,
            containerColor = TangemTheme.colors.background.action,
            iconTint = TangemTheme.colors.icon.attention,
        )
        HorizontalDivider(
            modifier = modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        missingNetworks.fastForEach { network ->
            key(network.id) {
                NetworkItems(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), networkItem = network)
            }
        }
    }
}

@Composable
private fun AvailableNetworksBlock(
    required: ImmutableList<WcNetworkInfoItem>,
    available: ImmutableList<WcNetworkInfoItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
            text = "Available networks",
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        required.fastForEach { network ->
            key(network.id) {
                NetworkItems(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), networkItem = network)
            }
        }
        available.fastForEach { network ->
            key(network.id) {
                NetworkItems(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), networkItem = network)
            }
        }
    }
}

@Composable
private fun NotAddedBlock(notAdded: ImmutableList<WcNetworkInfoItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
            text = "Not Added",
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        notAdded.fastForEach { network ->
            key(network.id) {
                NetworkItems(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), networkItem = network)
            }
        }
    }
}

@Composable
private fun NetworkItems(networkItem: WcNetworkInfoItem, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (networkItem) {
            is WcNetworkInfoItem.Checkable,
            is WcNetworkInfoItem.Checked,
            is WcNetworkInfoItem.Required,
            -> Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(networkItem.icon),
                contentDescription = null,
            )
            is WcNetworkInfoItem.ReadOnly -> Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors.button.secondary),
                painter = painterResource(networkItem.icon),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
        NetworkNameAndSymbol(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            name = networkItem.name,
            symbol = networkItem.symbol,
        )
        when (networkItem) {
            is WcNetworkInfoItem.Checkable -> TangemSwitch(
                onCheckedChange = networkItem.onCheckedChange,
                checked = networkItem.checked,
            )
            is WcNetworkInfoItem.Required -> Text(
                text = "Required",
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
            is WcNetworkInfoItem.Checked -> TangemSwitch(
                onCheckedChange = {},
                enabled = false,
                checked = true,
            )
            is WcNetworkInfoItem.ReadOnly -> Unit
        }
    }
}

@Composable
private fun NetworkNameAndSymbol(name: String, symbol: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = name,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = symbol, style = TangemTheme.typography.caption1, color = TangemTheme.colors.text.tertiary)
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcSelectNetworksContent_Preview(
    @PreviewParameter(WcSelectNetworksProvider::class)
    state: WcSelectNetworksUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
            containerColor = TangemTheme.colors.background.tertiary,
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            title = {
                TangemModalBottomSheetTitle(
                    title = stringReference("Choose wallet"),
                    onEndClick = {},
                    endIconRes = R.drawable.ic_close_24,
                )
            },
            content = {
                WcSelectNetworksContent(
                    modifier = Modifier
                        .background(TangemTheme.colors.background.tertiary)
                        .padding(horizontal = TangemTheme.dimens.spacing16),
                    state = state,
                )
            },
        )
    }
}

private class WcSelectNetworksProvider : CollectionPreviewParameterProvider<WcSelectNetworksUM>(
    collection = listOf(
        WcSelectNetworksUM(
            missing = persistentListOf(
                WcNetworkInfoItem.Required(
                    id = "ethereum",
                    name = "EthereumEthereumEthereumEthereumEthereumEthereumEthereumEthereum",
                    symbol = "ETH",
                    icon = R.drawable.img_eth_22,
                ),
                WcNetworkInfoItem.Required(
                    id = "bitcoin",
                    name = "Bitcoin",
                    symbol = "BTC",
                    icon = R.drawable.img_btc_22,
                ),
            ),
            required = persistentListOf(
                WcNetworkInfoItem.Checked(
                    id = "optimism",
                    name = "Optimism",
                    symbol = "OP",
                    icon = R.drawable.img_optimism_22,
                ),
            ),
            available = persistentListOf(
                WcNetworkInfoItem.Checkable(
                    id = "cardano",
                    name = "Cardano",
                    symbol = "ADA",
                    icon = R.drawable.img_cardano_22,
                    checked = false,
                    onCheckedChange = {},
                ),
                WcNetworkInfoItem.Checkable(
                    id = "polygon",
                    name = "Polygon",
                    symbol = "MATIC",
                    icon = R.drawable.img_polygon_22,
                    checked = true,
                    onCheckedChange = {},
                ),
            ),
            notAdded = persistentListOf(
                WcNetworkInfoItem.ReadOnly(
                    id = "solana",
                    name = "SOLANA",
                    symbol = "SOL",
                    icon = R.drawable.ic_solana_16,
                ),
                WcNetworkInfoItem.ReadOnly(
                    id = "avalanche",
                    name = "Avalanche",
                    symbol = "AVAX",
                    icon = R.drawable.ic_avalanche_22,
                ),
            ),
            onDone = {},
        ),
        WcSelectNetworksUM(
            missing = persistentListOf(),
            required = persistentListOf(
                WcNetworkInfoItem.Checked(
                    id = "optimism",
                    name = "Optimism",
                    symbol = "OP",
                    icon = R.drawable.img_optimism_22,
                ),
            ),
            available = persistentListOf(
                WcNetworkInfoItem.Checkable(
                    id = "cardano",
                    name = "Cardano",
                    symbol = "ADA",
                    icon = R.drawable.img_cardano_22,
                    checked = false,
                    onCheckedChange = {},
                ),
                WcNetworkInfoItem.Checkable(
                    id = "polygon",
                    name = "Polygon",
                    symbol = "MATIC",
                    icon = R.drawable.img_polygon_22,
                    checked = true,
                    onCheckedChange = {},
                ),
            ),
            notAdded = persistentListOf(
                WcNetworkInfoItem.ReadOnly(
                    id = "solana",
                    name = "SOLANA",
                    symbol = "SOL",
                    icon = R.drawable.ic_solana_16,
                ),
                WcNetworkInfoItem.ReadOnly(
                    id = "avalanche",
                    name = "Avalanche",
                    symbol = "AVAX",
                    icon = R.drawable.ic_avalanche_22,
                ),
            ),
            onDone = {},
        ),
    ),
)