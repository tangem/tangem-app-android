package com.tangem.domain.walletconnect.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class WcPairRequest(
    val uri: String,
    val source: Source,
    val userWalletId: UserWalletId,
) {
    enum class Source { QR, DEEPLINK, CLIPBOARD, ETC }
}