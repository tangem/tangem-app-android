package com.tangem.domain.exchange

sealed interface ExpressAvailabilityState {
    data object Available : ExpressAvailabilityState
    data object NotExchangeable : ExpressAvailabilityState
    data object NotOnrampable : ExpressAvailabilityState
    data object Loading : ExpressAvailabilityState
    data object Error : ExpressAvailabilityState
    data object AssetNotFound : ExpressAvailabilityState
}