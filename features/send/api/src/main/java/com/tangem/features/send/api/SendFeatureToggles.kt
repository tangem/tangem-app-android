package com.tangem.features.send.api

interface SendFeatureToggles {

    /** Enables the Tron gasless send flow (pay the network fee in USDT, sign two txs in one tap). */
    val isTronGaslessEnabled: Boolean
}