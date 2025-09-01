package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ENS
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

sealed class TokenReceiveNewAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token / Receive", event, params) {

    class ReceiveScreenOpened(
        token: String,
        blockchainName: String,
        ensStatus: AnalyticsParam.EnsStatus,
    ) : TokenReceiveNewAnalyticsEvent(
        event = "Receive Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchainName,
            ENS to ensStatus.value,
        ),
    )

    class ButtonCopyAddress(
        token: String,
        blockchainName: String,
        tokenReceiveSource: TokenReceiveCopyActionSource,
    ) : TokenReceiveNewAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchainName,
            SOURCE to tokenReceiveSource.name,
        ),
    )

    class ButtonCopyEns(
        token: String,
        blockchainName: String,
    ) : TokenReceiveNewAnalyticsEvent(
        event = "Button - ENS",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchainName,
        ),
    )

    class QrScreenOpened(
        token: String,
        blockchainName: String,
    ) : TokenReceiveNewAnalyticsEvent(
        event = "QR Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchainName,
        ),
    )
}

enum class TokenReceiveCopyActionSource {
    Main, Token, Receive, QR
}