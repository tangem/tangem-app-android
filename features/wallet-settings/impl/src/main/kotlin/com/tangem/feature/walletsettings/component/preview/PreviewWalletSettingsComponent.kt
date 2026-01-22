package com.tangem.feature.walletsettings.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.common.ui.userwallet.state.UserWalletItemUM.ImageState
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.AccountReorderUM
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM.Footer.AddAccountUM
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.ui.WalletSettingsScreen
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.hot.sdk.model.HotWalletId
import java.util.UUID

internal class PreviewWalletSettingsComponent : WalletSettingsComponent {

    private val accountName get() = stringReference(value = "Main account")

    private val accountItem: UserWalletItemUM
        get() = UserWalletItemUM(
            id = UUID.randomUUID().toString(),
            name = accountName,
            information = UserWalletItemUM.Information.Loaded(stringReference("12 tokens")),
            balance = UserWalletItemUM.Balance.Loaded("$726.04", false),
            isEnabled = true,
            onClick = { },
            imageState = ImageState.Account(
                name = accountName,
                icon = AccountIconPreviewData.randomAccountIcon(),
            ),
        )

    private val previewState = WalletSettingsUM(
        popBack = {},
        items = ItemsBuilder().buildItems(
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
            onManageTokensClick = {},
            isManageTokensAvailable = true,
            isNotificationsEnabled = true,
            onCheckedNotificationsChanged = {},
            onNotificationsDescriptionClick = {},
            isNotificationsPermissionGranted = false,
            onAccessCodeClick = {},
            onBackupClick = {},
            onCardSettingsClick = {},
            accountsUM = previewAccounts(),
            cardItem = previewCardBlock(),
        ),
        hasRequestPushNotificationsPermission = false,
        onPushNotificationPermissionGranted = {},
        accountReorderUM = AccountReorderUM(
            isDragEnabled = true,
            onMove = { _, _ -> },
            onDragStopped = {},
        ),
    )

    private fun previewAccounts() = listOf(
        WalletSettingsAccountsUM.Header(
            id = "accounts_header",
            text = resourceReference(R.string.common_accounts),
        ),
        WalletSettingsAccountsUM.Account(accountItem),
        WalletSettingsAccountsUM.Footer(
            id = "accounts_footer",
            addAccount = AddAccountUM(
                title = resourceReference(R.string.account_form_title_create),
                isAddAccountEnabled = true,
                onAddAccountClick = {},
            ),
            archivedAccounts = BlockUM(
                text = resourceReference(R.string.account_archived_accounts),
                iconRes = R.drawable.ic_archive_24,
                onClick = {},
            ),
            shouldShowDescription = true,
            description = resourceReference(R.string.account_reorder_description),
        ),
    )

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