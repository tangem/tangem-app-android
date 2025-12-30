package com.tangem.data.staking

import com.tangem.data.staking.converters.ethpool.P2PEthPoolStakingBalanceConverter
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.converter.StakingBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance

internal fun YieldBalanceWrapperDTO.toDomain(source: StatusSource = StatusSource.CACHE): StakingBalance {
    return StakingBalanceConverter(isCached = source == StatusSource.CACHE).convert(this)!!
}

internal fun P2PEthPoolAccountResponse.toDomain(
    source: StatusSource = StatusSource.CACHE,
): StakingBalance {
    return P2PEthPoolStakingBalanceConverter.convert(
        response = this,
        source = source,
    )
}