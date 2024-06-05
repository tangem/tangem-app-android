package com.tangem.domain.staking.error

sealed class StakingTokensError {

    data class DataError(val cause: Throwable) : StakingTokensError()
}