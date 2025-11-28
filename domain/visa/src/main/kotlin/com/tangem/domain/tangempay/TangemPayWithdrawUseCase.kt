package com.tangem.domain.tangempay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

interface TangemPayWithdrawUseCase {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
        receiverCexAddress: String,
    ): Either<UniversalError, Unit>
}