package com.tangem.domain.staking.model.stakekit

sealed class StakingError {

    // region stakekit errors

    data class StakekitApiError(
        val message: String?,
        val code: Int?,
    ) : StakingError()

    // endregion

    data class StakeKitUnknownError(
        val jsonString: String? = null,
    ) : StakingError()

    data class UnknownError(val message: String?) : StakingError()
}
