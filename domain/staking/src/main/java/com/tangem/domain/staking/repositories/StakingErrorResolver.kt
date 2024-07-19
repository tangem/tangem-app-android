package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.stakekit.StakingError

interface StakingErrorResolver {

    fun resolve(throwable: Throwable): StakingError
}
