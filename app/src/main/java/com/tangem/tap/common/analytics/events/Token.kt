package com.tangem.tap.common.analytics.events

/**
[REDACTED_AUTHOR]
 */
sealed class Token(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    class Refreshed : Token("Token", "Refreshed")
    class ButtonRemoveToken : Token("Token", "Button - Remove Token")
    class ButtonExplore : Token("Token", "Button - Explore")
    class ButtonBuy(token: String) : Token("Token", "Button - Buy", mapOf("Token" to token))
    class ButtonSell(token: String) : Token("Token", "Button - Sell", mapOf("Token" to token))
    class ButtonExchange(token: String) : Token("Token", "Button - Exchange", mapOf("Token" to token))
    class ButtonSend(token: String) : Token("Token", "Button - Send", mapOf("Token" to token))

    sealed class Recieve(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Recieve", event, params) {

        class ScreenOpened : Recieve("Recieve Screen Opened")
        class ButtonCopyAddress : Recieve("Button - Copy Address")
        class ButtonShareAddress : Recieve("Button - Share Address")
    }

    sealed class Send(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Send", event, params) {

        class ScreenOpened : Send("Send Screen Opened")
        class ButtonPaste : Send("Button - Paste")
        class ButtonQRCode : Send("Button - QR Code")
        class ButtonSwapCurrency : Send("Button - Swap Currency")

        class TransactionSent(token: AnalyticsParam.CurrencyType) : Send(
            event = "Transaction Sent",
            params = mapOf("Token" to token.value),
        )
    }

    sealed class Topup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Topup", event, params) {

        class ScreenOpened : Topup("Top Up Screen Opened")
        class P2PScreenOpened : Topup("P2P Screen Opened")
    }

    sealed class Withdraw(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Withdraw", event, params) {

        class ScreenOpened : Withdraw("Withdraw Screen Opened")
    }
}