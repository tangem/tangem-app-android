package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.StakingError
import com.tangem.domain.staking.model.Token
import com.tangem.domain.staking.model.transaction.StakingGasEstimate
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import java.math.BigDecimal

/**
 * Use case for staking gas estimation.
 */
class EstimateGasUseCase(
    private val stakingRepository: StakingRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    suspend operator fun invoke(
        integrationId: String,
        amount: BigDecimal,
        address: String,
        validatorAddress: String,
        token: Token,
    ): Either<StakingError, StakingGasEstimate> {
        return Either.catch {
            stakingRepository.estimateGas(
                integrationId = integrationId,
                amount = amount,
                address = address,
                validatorAddress = validatorAddress,
                token = token,
            )
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }
}
