package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Wallet dialog config. Used to show Decompose dialogs
 *
[REDACTED_AUTHOR]
 */
@Serializable
internal sealed interface WalletDialogConfig {

    @Serializable
    data class RenameWallet(val userWalletId: UserWalletId, val currentName: String) : WalletDialogConfig

    @Serializable
    data object AskForBiometry : WalletDialogConfig

    @Serializable
    data object AskForPushNotifications : WalletDialogConfig
}