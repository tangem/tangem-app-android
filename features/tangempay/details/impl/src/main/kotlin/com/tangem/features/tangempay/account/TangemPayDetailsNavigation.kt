package com.tangem.features.tangempay.account

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

    /** Multichain: lets the user pick which network to receive funds on. */
    @Serializable
    data class ChooseNetwork(val walletId: UserWalletId) : TangemPayDetailsNavigation()

    /** Multichain: the static "other networks" info sheet shown from [ChooseNetwork]. */
    @Serializable
    data object OtherNetworks : TangemPayDetailsNavigation()

    /**
     * Multichain: the pay-specific multi-token "Receive assets" sheet for an already-issued
     * (Available) network, shown from [ChooseNetwork]. The network's currencies and deposit address
     * are re-resolved from live status by walletId + the network's stable [networkRawId] rather than
     * carried here, since [com.tangem.domain.models.currency.CryptoCurrency] is not cleanly serializable.
     */
    @Serializable
    data class PaymentReceive(val walletId: UserWalletId, val networkRawId: String) : TangemPayDetailsNavigation()

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
        val userWalletId: UserWalletId,
        val paymentAccountAddress: String,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class VirtualAccountRequisites(
        val userWalletId: UserWalletId,
        val bankCredentials: BankCredentials,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class VaBankingDetailsError(
        val userWalletId: UserWalletId,
        val productInstanceId: String,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class TransactionDetails(
        val transaction: TangemPayTxHistoryItem,
        val isBalanceHidden: Boolean,
        val userWalletId: UserWalletId,
        val customerId: String,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class IssueAdditionalCard(
        val walletId: UserWalletId,
        val feeAmount: SerializedBigDecimal,
        val feeCurrency: SerializedCurrency,
        val fiatBalance: SerializedBigDecimal,
    ) : TangemPayDetailsNavigation()
}