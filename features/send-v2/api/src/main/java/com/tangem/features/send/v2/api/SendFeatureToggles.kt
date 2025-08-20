package com.tangem.features.send.v2.api

interface SendFeatureToggles {

    val isSendRedesignEnabled: Boolean
    val isNFTSendRedesignEnabled: Boolean
    val isSendWithSwapEnabled: Boolean
}