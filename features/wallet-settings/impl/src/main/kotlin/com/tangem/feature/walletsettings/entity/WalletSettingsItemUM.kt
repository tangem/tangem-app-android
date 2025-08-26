package com.tangem.feature.walletsettings.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.common.ui.userwallet.state.UserWalletItemUM.ImageState
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class WalletSettingsItemUM {

    abstract val id: String

    data class WithItems(
        override val id: String,
        val description: TextReference,
        val blocks: ImmutableList<BlockUM>,
    ) : WalletSettingsItemUM()

    data class WithSwitch(
        override val id: String,
        val title: TextReference,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : WalletSettingsItemUM()

    data class CardBlock(
        override val id: String,
        val title: TextReference,
        val text: TextReference,
        val isEnabled: Boolean,
        val imageState: ImageState,
        val onClick: () -> Unit,
    ) : WalletSettingsItemUM()

    data class DescriptionWithMore(
        override val id: String,
        val text: TextReference,
        val more: TextReference,
        val onClick: () -> Unit,
    ) : WalletSettingsItemUM()

    data class NotificationPermission(
        override val id: String,
        val title: TextReference,
        val description: TextReference,
    ) : WalletSettingsItemUM()

    data class UpgradeWallet(
        override val id: String,
        val title: TextReference,
        val description: TextReference,
        val onClick: () -> Unit,
        val onDismissClick: () -> Unit,
    ) : WalletSettingsItemUM()
}

@Immutable
internal sealed class WalletSettingsAccountsUM : WalletSettingsItemUM() {

    data class Header(
        override val id: String,
        val text: TextReference,
    ) : WalletSettingsAccountsUM()

    data class Account(
        override val id: String,
        val accountName: TextReference,
        val accountIconUM: CryptoPortfolioIconUM,
        val tokensInfo: TextReference,
        val networksInfo: TextReference,
        val onClick: () -> Unit,
    ) : WalletSettingsAccountsUM()

    data class Footer(
        override val id: String,
        val addAccount: AddAccountUM,
        val archivedAccounts: BlockUM,
        val description: TextReference,
    ) : WalletSettingsAccountsUM() {

        data class AddAccountUM(
            val title: TextReference,
            val addAccountEnabled: Boolean,
            val onAddAccountClick: () -> Unit,
        )
    }
}