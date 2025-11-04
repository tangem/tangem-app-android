package com.tangem.domain.pay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface TangemPayTopUpDataFactory {

    fun create(
        depositAddress: String,
        chainId: Int,
        cryptoBalance: BigDecimal,
        fiatBalance: BigDecimal,
    ): Either<UniversalError, TangemPayTopUpData>
}

data class TangemPayTopUpData(
    val walletId: UserWalletId,
    val currency: CryptoCurrency,
    val cryptoBalance: BigDecimal,
    val fiatBalance: BigDecimal,
    val depositAddress: String,
    val receiveAddress: List<ReceiveAddressModel>,
)