package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.*

interface StakingErrorResolver {

    fun resolve(throwable: Throwable): StakingError
}