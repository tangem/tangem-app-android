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

    // region p2p errors

    data class InvalidAmount(val message: String) : StakingError()

    data class DataError(val exception: Throwable) : StakingError()

    data class UnknownError(val exception: Throwable) : StakingError()

    // endregion

    data class DomainError(val message: String?) : StakingError()
}

sealed class StakingErrors {

    abstract val message: String

    data object MinimumAmountNotReachedError : StakingErrors() {
        override val message: String = "MinimumAmountNotReachedError"
    }
}