package com.tangem.data.staking.utils

import com.tangem.datasource.api.stakekit.models.request.Address
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.domain.staking.model.StakingID

/**
 * Factory for creating [YieldBalanceRequestBody]
 *
* [REDACTED_AUTHOR]
 */
internal object YieldBalanceRequestBodyFactory {

    fun create(stakingID: StakingID): YieldBalanceRequestBody {
        return YieldBalanceRequestBody(
            addresses = Address(
                address = stakingID.address,
                additionalAddresses = null, // todo fill additional addresses metadata if needed
                explorerUrl = "", // todo fill exporer url https://tangem.atlassian.net/browse/AND-7138
            ),
            args = YieldBalanceRequestBody.YieldBalanceRequestArgs(
                validatorAddresses = listOf(), // todo add validators https://tangem.atlassian.net/browse/AND-7138
            ),
            integrationId = stakingID.integrationId,
        )
    }
}
