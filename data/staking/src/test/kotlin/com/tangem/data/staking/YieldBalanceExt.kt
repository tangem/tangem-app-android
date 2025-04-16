package com.tangem.data.staking

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.stakekit.YieldBalance

internal fun YieldBalanceWrapperDTO.toDomain(source: StatusSource = StatusSource.CACHE): YieldBalance {
    return YieldBalanceConverter(source = source).convert(this)
}