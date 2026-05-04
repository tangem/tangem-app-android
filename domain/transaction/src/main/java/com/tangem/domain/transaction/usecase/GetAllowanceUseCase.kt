package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.AllowanceRepository
import java.math.BigDecimal

/**
 * Use case for retrieving the current allowance for a specific spender.
 */
class GetAllowanceUseCase(
    private val allowanceRepository: AllowanceRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
    ): Either<Throwable, BigDecimal> {
        return Either.catch {
            allowanceRepository.getAllowance(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                spenderAddress = spenderAddress,
            )
        }
    }
}