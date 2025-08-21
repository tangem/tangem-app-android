package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

@Serializable
sealed class InitScreenLaunchMode {

    @Serializable
    data object Standard : InitScreenLaunchMode()

    @Serializable
    data object WithCardScan : InitScreenLaunchMode()
}