package com.tangem.domain.walletconnect.model

import kotlinx.serialization.Serializable

@Serializable
data class WcPairRequest(
    val uri: String,
    val source: Source,
) {
    enum class Source { QR, DEEPLINK, CLIPBOARD, ETC }
}