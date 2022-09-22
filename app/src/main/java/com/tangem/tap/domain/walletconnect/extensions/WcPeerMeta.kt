package com.tangem.tap.domain.walletconnect.extensions

import com.trustwallet.walletconnect.models.WCPeerMeta

fun WCPeerMeta.isDappSupported(): Boolean {
    return !unsupportedDappsList.any { this.url.contains(it) }
}

private val unsupportedDappsList: List<String> = listOf("dydx.exchange")