package com.tangem.feature.walletsettings.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.userwallet.state.UserWalletItemUM.ImageState
import com.tangem.core.analytics.DummyAnalyticsEventHandler
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM.Footer.AddAccountUM
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
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
            isReferralAvailable = true,
            isLinkMoreCardsAvailable = true,
            isNFTFeatureEnabled = true,
            isNFTEnabled = true,
            onCheckedNFTChange = {},
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
            walletUpgradeDismissed = false,
            onUpgradeWalletClick = {},
            onDismissUpgradeWalletClick = {},
            accountsUM = previewAccounts(),
            cardItem = previewCardBlock(),
        ),
        requestPushNotificationsPermission = false,
        onPushNotificationPermissionGranted = {},
    )

    private fun previewAccounts() = buildList {
        WalletSettingsAccountsUM.Header(
            id = "accounts_header",
            text = resourceReference(R.string.common_accounts),
        ).let(::add)
        WalletSettingsAccountsUM.Account(
            id = "accountId",
            accountName = stringReference("Main account"),
            accountIconUM = AccountIconPreviewData.randomAccountIcon(),
            tokensInfo = stringReference("10 tokens"),
            networksInfo = stringReference("2 networks"),
            onClick = {},
        ).let(::add)
        WalletSettingsAccountsUM.Footer(
            id = "accounts_footer",
            addAccount = AddAccountUM(
                title = resourceReference(R.string.account_form_title_create),
                addAccountEnabled = true,
                onAddAccountClick = {},
            ),
            archivedAccounts = BlockUM(
                text = resourceReference(R.string.account_archived_accounts),
                iconRes = R.drawable.ic_archive_24,
                onClick = {},
            ),
            description = resourceReference(R.string.account_reorder_description),
        ).let(::add)
    }

    private fun previewCardBlock() = WalletSettingsItemUM.CardBlock(
        id = "wallet_name",
        title = resourceReference(id = R.string.user_wallet_list_rename_popup_placeholder),
        text = stringReference("Wallet Name"),
        isEnabled = true,
        onClick = { },
        imageState = ImageState.MobileWallet,
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