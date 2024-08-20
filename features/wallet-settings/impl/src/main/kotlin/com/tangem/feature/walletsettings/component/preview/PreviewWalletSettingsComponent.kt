package com.tangem.feature.walletsettings.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.ui.WalletSettingsScreen
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.features.managetokens.ManageTokensToggles

internal class PreviewWalletSettingsComponent : WalletSettingsComponent {

    private val previewState = WalletSettingsUM(
        popBack = {},
        items = ItemsBuilder(
            router = DummyRouter(),
            manageTokensToggles = object : ManageTokensToggles {
                override val isFeatureEnabled: Boolean = true
            },
        ).buildItems(
            userWalletId = UserWalletId("011"),
            userWalletName = "My Wallet",
            isReferralAvailable = true,
            isLinkMoreCardsAvailable = true,
            renameWallet = {},
            forgetWallet = {},
            onLinkMoreCardsClick = {},
        ),
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
