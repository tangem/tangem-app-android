package com.tangem.data.staking.utils

import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.domain.models.staking.StakingID

/**
 * Factory for creating [YieldBalanceRequestBody]
 *
[REDACTED_AUTHOR]
 */
internal object YieldBalanceRequestBodyFactory {

    fun create(stakingID: StakingID): YieldBalanceRequestBody {
        return YieldBalanceRequestBody(
            addresses = YieldBalanceRequestBodyAddressFactory.create(stakingID),
            args = YieldBalanceRequestBody.YieldBalanceRequestArgs(
                validatorAddresses = listOf(), // todo add validators [REDACTED_JIRA]
            ),
            integrationId = stakingID.integrationId,
        )
    }
}