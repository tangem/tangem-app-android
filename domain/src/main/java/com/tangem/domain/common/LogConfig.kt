package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

data class LogConfig(
    val imageLoader: Boolean,
    val storeAction: Boolean,
    val zendesk: Boolean,
    val network: NetworkLogConfig,
) {

    companion object {
        fun buildBased(): LogConfig = LogConfig(
            imageLoader = BuildConfig.DEBUG,
            storeAction = BuildConfig.DEBUG,
            zendesk = BuildConfig.DEBUG,
            network = NetworkLogConfig.buildBased(),
        )

        fun custom(): LogConfig = LogConfig(
            imageLoader = false,
            storeAction = false,
            zendesk = false,
            network = NetworkLogConfig.custom(),
        )
    }
}

data class NetworkLogConfig(
    val mercuryoService: Boolean,
    val moonPayService: Boolean,
    val tangemTechService: Boolean,
    val blockchainSdkNetwork: Boolean,
) {

    companion object {
        fun buildBased(): NetworkLogConfig = NetworkLogConfig(
            mercuryoService = BuildConfig.DEBUG,
            moonPayService = BuildConfig.DEBUG,
            tangemTechService = BuildConfig.DEBUG,
            blockchainSdkNetwork = BuildConfig.DEBUG,
        )

        fun custom(): NetworkLogConfig = NetworkLogConfig(
            mercuryoService = false,
            moonPayService = false,
            tangemTechService = false,
            blockchainSdkNetwork = true,
        )
    }
}