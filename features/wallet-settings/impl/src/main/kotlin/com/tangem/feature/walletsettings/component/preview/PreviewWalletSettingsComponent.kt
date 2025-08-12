package com.tangem.feature.walletsettings.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.analytics.DummyAnalyticsEventHandler
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.ui.WalletSettingsScreen
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.hot.sdk.model.HotWalletId

internal class PreviewWalletSettingsComponent : WalletSettingsComponent {

    private val previewState = WalletSettingsUM(
        popBack = {},
        items = ItemsBuilder(
            router = DummyRouter(),
            analyticsEventHandler = DummyAnalyticsEventHandler(),
        ).buildItems(
            userWallet = UserWallet.Hot(
                walletId = UserWalletId("011"),
                name = "My Wallet",
                hotWalletId = HotWalletId("", HotWalletId.AuthType.NoPassword),
                wallets = null,
                backedUp = false,
            ),
            userWalletName = "My Wallet",
            isReferralAvailable = true,
            isLinkMoreCardsAvailable = true,
            isRenameWalletAvailable = false,
            isNFTFeatureEnabled = true,
            isNFTEnabled = true,
            onCheckedNFTChange = {},
            renameWallet = {},
            forgetWallet = {},
            onLinkMoreCardsClick = {},
            onReferralClick = {},
            isManageTokensAvailable = true,
            isNotificationsEnabled = true,
            isNotificationsFeatureEnabled = true,
            onCheckedNotificationsChanged = {},
            onNotificationsDescriptionClick = {},
            isNotificationsPermissionGranted = false,
            onAccessCodeClick = {},
        ),
        requestPushNotificationsPermission = false,
        onPushNotificationPermissionGranted = {},
    )

    @Composable
    override fun Content(modifier: Modifier) {
        WalletSettingsScreen(
            modifier = modifier,
            state = previewState,
            dialog = {},
        )
    }
}