package com.tangem.domain.onramp.model.error

sealed class OnrampPairsError : Throwable() {
    class PairsNotFound : OnrampPairsError()
}