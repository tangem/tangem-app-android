package com.tangem.data.tangempay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tangempay.TangemPayWithdrawExchangeState
import com.tangem.domain.tangempay.WithdrawalResult
import com.tangem.domain.tangempay.repository.TangemPayWithdrawRepository
import com.tangem.domain.tangempay.TangemPayWithdrawWithSwapUseCase
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultTangemPayWithdrawWithSwapUseCase @Inject constructor(
    private val repository: TangemPayWithdrawRepository,
) : TangemPayWithdrawWithSwapUseCase {

    override suspend fun invoke(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        sourceCurrency: CryptoCurrency,
        receiverCexAddress: String,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult> {
        return repository.withdrawWithSwap(
            userWallet = userWallet,
            cryptoAmount = cryptoAmount,
            receiverAddress = receiverCexAddress,
            sourceCurrency = sourceCurrency,
            exchangeData = exchangeData,
        )
    }
}