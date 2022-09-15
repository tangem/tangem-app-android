package com.tangem.domain.common

object LogConfig {
    const val imageLoader: Boolean = false
    const val storeAction: Boolean = false
    const val zendesk: Boolean = false
    val network: NetworkLogConfig = NetworkLogConfig
}

object NetworkLogConfig {
    const val mercuryoService: Boolean = false
    const val moonPayService: Boolean = false
    const val tangemTechService: Boolean = false
    const val blockchainSdkNetwork: Boolean = false
}