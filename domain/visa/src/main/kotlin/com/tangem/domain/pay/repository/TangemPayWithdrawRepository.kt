package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayWithdrawExchangeState
import com.tangem.domain.pay.WithdrawalResult
import java.math.BigDecimal

interface TangemPayWithdrawRepository {

    suspend fun withdraw(
        userWallet: UserWallet,
        receiverAddress: String,
        cryptoAmount: BigDecimal,
        cryptoCurrencyId: CryptoCurrency.RawID,
        exchangeData: TangemPayWithdrawExchangeState,
    ): Either<UniversalError, WithdrawalResult>

    suspend fun hasWithdrawOrder(userWallet: UserWallet): Boolean

    suspend fun pollWithdrawOrdersIfNeeds(userWallet: UserWallet)
}