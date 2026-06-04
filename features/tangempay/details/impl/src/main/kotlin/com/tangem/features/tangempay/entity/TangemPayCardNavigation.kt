package com.tangem.features.tangempay.entity

import com.tangem.domain.models.TokenReceiveConfig
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
    data object ReissueCard : TangemPayCardNavigation()

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
    ) : TangemPayCardNavigation()

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : TangemPayCardNavigation()
}