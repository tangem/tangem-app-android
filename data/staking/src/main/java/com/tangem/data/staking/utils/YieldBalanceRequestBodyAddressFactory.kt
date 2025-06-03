package com.tangem.data.staking.utils

import com.tangem.datasource.api.stakekit.models.request.Address
import com.tangem.domain.staking.model.StakingID

/**
 * Factory for creating [Address]
 *
* [REDACTED_AUTHOR]
 */
internal object YieldBalanceRequestBodyAddressFactory {

    fun create(stakingId: StakingID): Address {
        return Address(
            address = stakingId.address,
            additionalAddresses = null, // todo fill additional addresses metadata if needed
            explorerUrl = "", // todo fill exporer url https://tangem.atlassian.net/browse/AND-7138
        )
    }
}
