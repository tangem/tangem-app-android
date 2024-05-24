package com.tangem.data.staking

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.repositories.StakingRepository

internal class DefaultStakingRepository(
    private val stakeKitApi: StakeKitApi,
) : StakingRepository {

    override suspend fun getEntryInfo(integrationId: String): StakingEntryInfo {
        val yield = stakeKitApi.getSingleYield(integrationId).getOrThrow()

        // TODO staking add converter
        return StakingEntryInfo(
            percent = yield.apy.toString(),
            periodInDays = yield.metadata.cooldownPeriod.days,
            tokenSymbol = yield.token.symbol
        )
    }
}
