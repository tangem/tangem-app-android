package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.repositories.StakingRepository
import java.math.BigDecimal

class GetActionRequirementAmountUseCase(
    private val stakingRepository: StakingRepository,
) {

    operator fun invoke(integrationId: String, actionType: StakingActionType): Either<Throwable, BigDecimal?> =
        Either.catch {
            stakingRepository.getActionRequirementAmount(integrationId, actionType)
        }
}