package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface TangemPaySwapRepository {

    suspend fun withdraw(
        userWalletId: UserWalletId,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
    ): Either<UniversalError, Unit>
}