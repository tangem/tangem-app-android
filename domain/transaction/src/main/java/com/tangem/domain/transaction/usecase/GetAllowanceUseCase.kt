package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

class GetAllowanceUseCase(
    private val transactionRepository: TransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
    ): Either<Throwable, BigDecimal> {
        return Either.catch {
            transactionRepository.getAllowance(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency as CryptoCurrency.Token,
                spenderAddress = spenderAddress,
            )
        }
    }
}