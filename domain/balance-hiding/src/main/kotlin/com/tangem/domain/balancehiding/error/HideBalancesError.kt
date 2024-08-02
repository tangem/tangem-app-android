package com.tangem.domain.balancehiding.error

sealed class HideBalancesError {

    data object HidingDisabled : HideBalancesError()

    data class DataError(val cause: Throwable) : HideBalancesError()
}