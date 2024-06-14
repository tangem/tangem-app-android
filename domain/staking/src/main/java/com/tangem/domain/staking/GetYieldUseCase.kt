package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Use case for getting staking yield for staking scenario start.
 */
class GetYieldUseCase(private val stakingRepository: StakingRepository) {

    suspend operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Either<Throwable, Yield> {
        return Either.catch { stakingRepository.getYield(cryptoCurrencyId, symbol) }
    }
}