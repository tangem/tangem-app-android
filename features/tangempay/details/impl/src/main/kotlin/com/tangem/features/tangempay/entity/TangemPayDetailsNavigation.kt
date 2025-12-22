package com.tangem.features.tangempay.entity

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.serialization.SerializedBigDecimal
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
        val chainId: Int,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class TransactionDetails(
        val transaction: TangemPayTxHistoryItem,
        val isBalanceHidden: Boolean,
    ) : TangemPayDetailsNavigation()

    @Serializable
    data class ViewPinCode(
        val userWalletId: UserWalletId,
        val cardId: String,
    ) : TangemPayDetailsNavigation()
}