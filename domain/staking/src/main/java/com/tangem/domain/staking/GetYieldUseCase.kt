package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.raise.catch
import com.tangem.domain.staking.model.StakingError
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Use case for getting staking yield for staking scenario start.
 */
class GetYieldUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Either<StakingError, Yield> {
        return Either
            .catch { stakingRepository.getYield(cryptoCurrencyId, symbol) }
            .mapLeft { stakingErrorResolver.resolve(it) }
    }
}