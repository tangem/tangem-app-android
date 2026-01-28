package com.tangem.domain.tangempay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalResult
import java.math.BigDecimal

interface TangemPayWithdrawUseCase {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
        receiverCexAddress: String,
    ): Either<UniversalError, WithdrawalResult>
}