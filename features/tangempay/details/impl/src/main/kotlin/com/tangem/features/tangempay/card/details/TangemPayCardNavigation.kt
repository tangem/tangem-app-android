package com.tangem.features.tangempay.card.details

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayCardNavigation {
    @Serializable
    data class ViewPinCode(
        val userWalletId: UserWalletId,
        val cardId: String,
    ) : TangemPayCardNavigation()

    @Serializable
    data class ReissueCard(val cardId: String) : TangemPayCardNavigation()

    @Serializable
    data class CloseCard(
        val userWalletId: UserWalletId,
        val cardId: String,
    ) : TangemPayCardNavigation()

    @Serializable
    data class AddFunds(
        val walletId: UserWalletId,
        val cryptoBalance: SerializedBigDecimal,
        val fiatBalance: SerializedBigDecimal,
        val depositAddress: String,
        val cryptoCurrency: CryptoCurrency,
        val virtualAccountOnramp: VirtualAccountOnramp?,
    ) : TangemPayCardNavigation()

    @Serializable
    data class VirtualAccountDeposit(
        val virtualAccountOnramp: VirtualAccountOnramp,
        val userWalletId: UserWalletId,
        val paymentAccountAddress: String,
    ) : TangemPayCardNavigation()

    @Serializable
    data class VirtualAccountRequisites(
        val userWalletId: UserWalletId,
        val bankCredentials: BankCredentials,
    ) : TangemPayCardNavigation()

    @Serializable
    data class VaBankingDetailsError(
        val userWalletId: UserWalletId,
        val productInstanceId: String,
    ) : TangemPayCardNavigation()

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : TangemPayCardNavigation()

    /** Multichain: lets the user pick which network to receive funds on. */
    @Serializable
    data class ChooseNetwork(val walletId: UserWalletId) : TangemPayCardNavigation()

    /** Multichain: the static "other networks" info sheet shown from [ChooseNetwork]. */
    @Serializable
    data object OtherNetworks : TangemPayCardNavigation()

    /**
     * Multichain: the pay-specific multi-token "Receive assets" sheet for an already-issued
     * (Available) network, shown from [ChooseNetwork]. The network's currencies and deposit address
     * are re-resolved from live status by walletId + the network's stable [networkRawId] rather than
     * carried here, since [com.tangem.domain.models.currency.CryptoCurrency] is not cleanly serializable.
     */
    @Serializable
    data class PaymentReceive(val walletId: UserWalletId, val networkRawId: String) : TangemPayCardNavigation()
}