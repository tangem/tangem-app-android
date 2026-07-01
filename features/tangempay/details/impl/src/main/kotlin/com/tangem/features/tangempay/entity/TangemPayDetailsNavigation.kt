package com.tangem.features.tangempay.entity

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayDetailsNavigation {

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : TangemPayDetailsNavigation()

    @Serializable
    data class AddFunds(
        val walletId: UserWalletId,
        val cryptoBalance: SerializedBigDecimal,
        val fiatBalance: SerializedBigDecimal,
        val depositAddress: String,
        val cryptoCurrency: CryptoCurrency,
        val virtualAccountOnramp: VirtualAccountOnramp?,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class VirtualAccountDeposit(
        val virtualAccountOnramp: VirtualAccountOnramp,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class VirtualAccountRequisites(
        val userWalletId: UserWalletId,
        val bankCredentials: BankCredentials,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class TransactionDetails(
        val transaction: TangemPayTxHistoryItem,
        val isBalanceHidden: Boolean,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class IssueAdditionalCard(
        val walletId: UserWalletId,
        val feeAmount: SerializedBigDecimal,
        val feeCurrency: SerializedCurrency,
        val fiatBalance: SerializedBigDecimal,
    ) : TangemPayDetailsNavigation()
}