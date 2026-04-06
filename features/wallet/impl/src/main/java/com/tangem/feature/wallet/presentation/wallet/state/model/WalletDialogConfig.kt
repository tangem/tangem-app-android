package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.serialization.BigDecimalSerializer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.details.TokenAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
    data class TokenActionList(
        val actionList: ImmutableList<TokenActionButtonUM>,
    ) : WalletDialogConfig

    @Serializable
    data class YieldSupplyWarning(
        val cryptoCurrency: CryptoCurrency,
        val tokenAction: TokenAction,
        val onWarningAcknowledged: (TokenAction) -> Unit,
    ) : WalletDialogConfig

    @Serializable
    data class KycRejected(val walletId: UserWalletId, val customerId: String) : WalletDialogConfig

    @Serializable
    data class OrganizeTokens(val userWalletId: UserWalletId) : WalletDialogConfig

    @Serializable
    data class NetworkSelection(
        val address: String,
        val amount: @Serializable(BigDecimalSerializer::class) BigDecimal?,
        val memo: String?,
        val walletGroups: List<WalletGroupData>,
    ) : WalletDialogConfig {

        @Serializable
        data class WalletGroupData(
            val userWalletId: UserWalletId,
            val walletName: String,
            val accounts: List<AccountGroupData>,
        )

        @Serializable
        data class AccountGroupData(
            val accountId: AccountId,
            val accountName: AccountName,
            val currencies: List<CryptoCurrency>,
            val hiddenTokensCount: Int = 0,
        )
    }
}