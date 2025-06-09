package com.tangem.domain.tokens.utils

import com.tangem.domain.models.network.NetworkStatus

/** Extract address from [networkStatus] */
internal fun extractAddress(networkStatus: NetworkStatus?): String? {
    return when (val value = networkStatus?.value) {
        is NetworkStatus.NoAccount -> value.address.defaultAddress.value
        is NetworkStatus.Unreachable -> value.address?.defaultAddress?.value
        is NetworkStatus.Verified -> value.address.defaultAddress.value
        else -> null
    }
}