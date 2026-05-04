package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.AllowanceRepository
import com.tangem.domain.transaction.models.AllowanceInfo
import java.math.BigDecimal

/**
 * Use case for retrieving the allowance information for a specific spender and required amount.
 */
class GetAllowanceInfoUseCase(
    private val allowanceRepository: AllowanceRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
        requiredAmount: BigDecimal,
    ): Either<Throwable, AllowanceInfo> {
        return Either.catch {
            allowanceRepository.getAllowanceInfo(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                spenderAddress = spenderAddress,
                requiredAmount = requiredAmount,
            )
        }
    }
}