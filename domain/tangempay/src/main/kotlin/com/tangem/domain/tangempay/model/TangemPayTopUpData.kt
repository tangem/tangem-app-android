package com.tangem.domain.tangempay.model

import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

data class TangemPayTopUpData(
    val walletId: UserWalletId,
    val currency: CryptoCurrency,
    val cryptoBalance: BigDecimal,
    val fiatBalance: BigDecimal,
    val depositAddress: String,
    val receiveAddress: List<ReceiveAddressModel>,
)