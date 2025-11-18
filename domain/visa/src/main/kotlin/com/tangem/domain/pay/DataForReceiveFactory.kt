package com.tangem.domain.pay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface DataForReceiveFactory {

    fun getDataForReceive(depositAddress: String, chainId: Int): Either<UniversalError, DataForReceive>
}

data class DataForReceive(
    val walletId: UserWalletId,
    val currency: CryptoCurrency,
    val receiveAddress: List<ReceiveAddressModel>,
)