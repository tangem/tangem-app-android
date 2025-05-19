package com.tangem.domain.onramp.model.error

sealed class OnrampPairsError : Throwable() {
    data object PairsNotFound : OnrampPairsError()
}