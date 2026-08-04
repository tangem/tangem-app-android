package com.tangem.features.send.api

interface SendFeatureToggles {

    val isTronGaslessEnabled: Boolean

    val isHighFeeWarningEnabled: Boolean
}