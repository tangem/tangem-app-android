package com.tangem.domain.walletconnect.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
data class WcPairRequest(
    val uri: String,
    val source: Source,
    val userWalletId: UserWalletId,
    val screen: Screen? = null,
) {
    enum class Source { QR, DEEPLINK, CLIPBOARD, ETC }

    enum class Screen(val analyticsName: String) {
        MAIN("Main Screen"),
        WALLET_CONNECT("Wallet Connect Screen"),
    }
}