package com.tangem.domain.staking.model.stakekit

import java.math.BigDecimal

sealed class StakingError {

    // region stakekit errors

    data class StakeKitApiError(
        val message: String?,
        val code: Int?,
        val details: ErrorDetails?,
        val methodName: String?,
    ) : StakingError() {

        data class ErrorDetails(
            val amount: BigDecimal?,
        )
    }

    data class StakeKitUnknownError(
        val jsonString: String? = null,
    ) : StakingError()

    // endregion

    data class DomainError(val message: String?) : StakingError()
}

sealed class StakingErrors {

    abstract val message: String

    data object MinimumAmountNotReachedError : StakingErrors() {
        override val message: String = "MinimumAmountNotReachedError"
    }
}