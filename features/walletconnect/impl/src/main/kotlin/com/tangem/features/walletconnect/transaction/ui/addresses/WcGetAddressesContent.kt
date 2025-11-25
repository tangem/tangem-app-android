package com.tangem.features.walletconnect.transaction.ui.addresses

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.addresses.WcGetAddressesUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.ui.common.WcNetworkItem
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestButtons

@Composable
internal fun WcGetAddressesContent(
    state: WcGetAddressesUM,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_get_addresses_title),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            Column(
                modifier = modifier.padding(horizontal = 16.dp),
            ) {
                // dApp info section
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(color = TangemTheme.colors.background.action)
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    WcSmallTitleItem(R.string.wc_request_from)
                    WcAppInfoItem(
                        iconUrl = state.appInfo.appIcon,
                        title = state.appInfo.appName,
                        subtitle = state.appInfo.appSubtitle,
                        verifiedDAppState = state.appInfo.verifiedState,
                    )
                }

                // Network info section
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color = TangemTheme.colors.background.action)
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    WcNetworkItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        networkInfo = state.networkInfo,
                    )
                }

                // Addresses section
                if (state.addresses.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color = TangemTheme.colors.background.action)
                            .fillMaxWidth()
                            .animateContentSize(),
                    ) {
                        WcSmallTitleItem(R.string.wc_get_addresses_addresses_title)
                        state.addresses.forEachIndexed { index, addressInfo ->
                            if (index > 0) {
                                DividerWithPadding(start = 12.dp, end = 12.dp)
                            }
                            WcAddressInfoItem(
                                address = addressInfo.address,
                                intention = addressInfo.intention,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            )
                        }
                    }
                }
            }
        },
        footer = {
            WcTransactionRequestButtons(
                modifier = Modifier.padding(16.dp),
                onDismiss = state.onReject,
                onClickActiveButton = state.onApprove,
                activeButtonText = resourceReference(R.string.common_approve),
                isLoading = state.isLoading,
                walletInteractionIcon = state.walletInteractionIcon,
                validationResult = null,
            )
        },
    )
}

@Composable
private fun WcAddressInfoItem(
    address: String,
    intention: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (intention != null) {
            Text(
                text = intention.replaceFirstChar { it.uppercase() },
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = address,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// region Preview

@Preview
@Composable
private fun WcGetAddressesContentPreview(
    @PreviewParameter(WcGetAddressesStateProvider::class) state: WcGetAddressesUM,
) {
    TangemThemePreview {
        WcGetAddressesContent(
            state = state,
            onDismiss = {},
        )
    }
}

private class WcGetAddressesStateProvider : CollectionPreviewParameterProvider<WcGetAddressesUM>(
    listOf(
        // Without addresses (initial state)
        WcGetAddressesUM(
            appInfo = WcTransactionAppInfoContentUM(
                appName = "Uniswap",
                appIcon = "",
                appSubtitle = "uniswap.org",
                verifiedState = VerifiedDAppState.Verified(onVerifiedClick = {}),
            ),
            networkInfo = WcNetworkInfoUM(
                name = "Bitcoin",
                iconRes = R.drawable.img_btc_22,
            ),
            addresses = emptyList(),
            isLoading = false,
            walletInteractionIcon = R.drawable.ic_tangem_24,
            onApprove = {},
            onReject = {},
        ),
        // With addresses
        WcGetAddressesUM(
            appInfo = WcTransactionAppInfoContentUM(
                appName = "Magic Eden",
                appIcon = "",
                appSubtitle = "magiceden.io",
                verifiedState = VerifiedDAppState.Unknown,
            ),
            networkInfo = WcNetworkInfoUM(
                name = "Bitcoin",
                iconRes = R.drawable.img_btc_22,
            ),
            addresses = listOf(
                WcGetAddressesUM.AddressInfo(
                    address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                    intention = "payment",
                ),
                WcGetAddressesUM.AddressInfo(
                    address = "bc1q9h5yjfmefwm4y6z8rgkqj3e8k7rq9k7ymxrrtw",
                    intention = "ordinal",
                ),
            ),
            isLoading = false,
            walletInteractionIcon = R.drawable.ic_tangem_24,
            onApprove = {},
            onReject = {},
        ),
        // Loading state
        WcGetAddressesUM(
            appInfo = WcTransactionAppInfoContentUM(
                appName = "OpenSea",
                appIcon = "",
                appSubtitle = "opensea.io",
                verifiedState = VerifiedDAppState.Verified(onVerifiedClick = {}),
            ),
            networkInfo = WcNetworkInfoUM(
                name = "Bitcoin",
                iconRes = R.drawable.img_btc_22,
            ),
            addresses = listOf(
                WcGetAddressesUM.AddressInfo(
                    address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                    intention = null,
                ),
            ),
            isLoading = true,
            walletInteractionIcon = R.drawable.ic_tangem_24,
            onApprove = {},
            onReject = {},
        ),
    ),
)

// endregion