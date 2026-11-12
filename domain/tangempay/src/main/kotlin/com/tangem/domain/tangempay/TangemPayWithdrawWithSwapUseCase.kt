package com.tangem.domain.tangempay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import java.math.BigDecimal

interface TangemPayWithdrawWithSwapUseCase {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        sourceCurrency: CryptoCurrency,
        receiverCexAddress: String,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult>
}