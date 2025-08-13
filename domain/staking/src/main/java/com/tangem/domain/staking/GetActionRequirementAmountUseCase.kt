package com.tangem.domain.staking

import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.models.staking.action.StakingActionType
import java.math.BigDecimal

class GetActionRequirementAmountUseCase {

    operator fun invoke(integrationId: String, actionType: StakingActionType): BigDecimal? {
        return if (StakingIntegrationID.EthereumToken.Polygon.value == integrationId &&
            actionType == StakingActionType.CLAIM_REWARDS
        ) {
            BigDecimal.ONE
        } else {
            null
        }
    }
}