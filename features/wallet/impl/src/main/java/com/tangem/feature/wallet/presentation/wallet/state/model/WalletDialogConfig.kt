package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.details.TokenAction
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

    @Serializable
    data class TokenReceive(val tokenReceiveConfig: TokenReceiveConfig) : WalletDialogConfig

    @Serializable
    data class YieldSupplyWarning(
        val cryptoCurrency: CryptoCurrency,
        val tokenAction: TokenAction,
        val onWarningAcknowledged: (TokenAction) -> Unit,
    ) : WalletDialogConfig
}