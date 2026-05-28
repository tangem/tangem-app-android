package com.tangem.data.pay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.tangempay.TangemPayWithdrawWithSwapUseCase
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultTangemPayWithdrawWithSwapUseCase @Inject constructor(
    private val repository: TangemPayWithdrawRepository,
) : TangemPayWithdrawWithSwapUseCase {

    override suspend fun invoke(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
        receiverCexAddress: String,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult> {
        return repository.withdrawWithSwap(
            userWallet = userWallet,
            cryptoAmount = cryptoAmount,
            receiverAddress = receiverCexAddress,
            cryptoCurrencyId = cryptoCurrencyId,
            exchangeData = exchangeData,
        )
    }
}