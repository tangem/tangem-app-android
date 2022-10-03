package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    val imageLoader: Boolean = false
    val storeAction: Boolean = BuildConfig.DEBUG
    val zendesk: Boolean = false
    val network: NetworkLogConfig = NetworkLogConfig
}

object NetworkLogConfig {
    val mercuryoService: Boolean = false
    val moonPayService: Boolean = false
    val tangemTechService: Boolean = BuildConfig.DEBUG
    val blockchainSdkNetwork: Boolean = BuildConfig.DEBUG
}
