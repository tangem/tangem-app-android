package com.tangem.domain.staking.model.stakekit

sealed class StakingError {

    // region stakekit errors

    data class StakeKitApiError(
        val message: String?,
        val code: Int?,
        val methodName: String?,
    ) : StakingError()

    data class StakeKitUnknownError(
        val jsonString: String? = null,
    ) : StakingError()

    // endregion

    data class DomainError(val message: String?) : StakingError()
}