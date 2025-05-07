package com.tangem.features.walletconnect.transaction.ui.sign

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionItemUM
import com.tangem.features.walletconnect.transaction.ui.common.*

@Composable
internal fun WcSignTransactionModalBottomSheetContent(
    state: WcSignTransactionItemUM,
    onClickTransactionRequest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action)
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            WcSmallTitleItem(R.string.wc_request_from)
            WcAppInfoItem(
                iconUrl = state.appInfo.appIcon,
                title = state.appInfo.appName,
                subtitle = state.appInfo.appSubtitle,
                isVerified = state.appInfo.isVerified,
            )
            DividerWithPadding(start = 0.dp, end = 0.dp)
            WcTransactionRequestItem(
                iconRes = R.drawable.ic_doc_new_24,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickTransactionRequest() }
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
        Column(modifier = Modifier.padding(top = TangemTheme.dimens.spacing16)) {
            WcSignTransactionItems(state)
            WcTransactionRequestButtons(
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16),
                onDismiss = state.onDismiss,
                onClickActiveButton = state.onSign,
                activeButtonText = resourceReference(R.string.common_sign),
                isLoading = state.isLoading,
            )
        }
    }
}

@Composable
private fun WcSignTransactionItems(state: WcSignTransactionItemUM) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        val itemsModifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing12)

        DividerWithPadding(start = 0.dp, end = 0.dp)
        WcWalletItem(
            modifier = itemsModifier,
            walletName = state.walletName,
        )
        DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
        WcNetworkItem(
            modifier = itemsModifier,
            networkInfo = state.networkInfo,
        )
        if (!state.addressText.isNullOrEmpty()) {
            DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
            WcAddressItem(
                modifier = itemsModifier,
                addressText = state.addressText,
            )
        }
    }
}

@Composable
internal fun DividerWithPadding(start: Dp, end: Dp) {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = start,
            end = end,
        ),
        thickness = TangemTheme.dimens.size1,
        color = TangemTheme.colors.stroke.primary,
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSignTransactionBottomSheetPreview(
    @PreviewParameter(WcSignTransactionStateProvider::class) state: WcSignTransactionItemUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcSignTransactionItemUM>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.wallet_connect_title),
                    endIconRes = R.drawable.ic_close_24,
                    onEndClick = {},
                    startIconRes = null,
                    onStartClick = {},
                )
            },
            content = {
                WcSignTransactionModalBottomSheetContent(state = state, onClickTransactionRequest = {})
            },
        )
    }
}

private class WcSignTransactionStateProvider : CollectionPreviewParameterProvider<WcSignTransactionItemUM>(
    listOf(
        WcSignTransactionItemUM(
            onDismiss = {},
            onSign = {},
            appInfo = WcTransactionAppInfoContentUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
            ),
            walletName = "Tangem 2.0",
            networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
        ),
        WcSignTransactionItemUM(
            onDismiss = {},
            onSign = {},
            appInfo = WcTransactionAppInfoContentUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
            ),
            walletName = "Tangem 2.0",
            addressText = "0x345FF...34FA",
            networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
        ),
    ),
)