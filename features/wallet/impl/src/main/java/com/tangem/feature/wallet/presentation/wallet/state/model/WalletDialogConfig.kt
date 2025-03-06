package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Wallet dialog config. Used to show Decompose dialogs
 *
 * @author Andrew Khokhlov on 07/02/2025
 */
@Serializable
internal sealed interface WalletDialogConfig {

    @Serializable
    data class RenameWallet(val userWalletId: UserWalletId, val currentName: String) : WalletDialogConfig

    @Serializable
    data object AskForBiometry : WalletDialogConfig
}
