package com.tangem.tap.common.analytics.events

import com.tangem.tap.common.analytics.events.AnalyticsParam.CurrencyType

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class Token(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    class Refreshed : Token("Token", "Refreshed")
    class ButtonExplore : Token("Token", "Button - Explore")

    class ButtonRemoveToken(type: CurrencyType) : Token(
        "Token",
        "Button - Remove Token",
        params = mapOf("Token" to type.value),
    )

    class ButtonBuy(type: CurrencyType) : Token(
        category = "Token",
        event = "Button - Buy",
        params = mapOf("Token" to type.value),
    )

    class ButtonSell(type: CurrencyType) : Token(
        category = "Token",
        event = "Button - Sell",
        params = mapOf("Token" to type.value),
    )

    class ButtonExchange(type: CurrencyType) : Token(
        category = "Token",
        event = "Button - Exchange",
        params = mapOf("Token" to type.value),
    )

    class ButtonSend(type: CurrencyType) : Token(
        category = "Token",
        event = "Button - Send",
        params = mapOf("Token" to type.value),
    )

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
        error: Throwable? = null,
    ) : Token("Token / Send", event, params, error) {

        class ScreenOpened : Send("Send Screen Opened")
        class ButtonPaste : Send("Button - Paste")
        class ButtonQRCode : Send("Button - QR Code")
        class ButtonSwapCurrency : Send("Button - Swap Currency")

        class TransactionSent(type: CurrencyType, error: Throwable? = null) : Send(
            event = "Transaction Sent",
            params = mapOf("Token" to type.value),
            error = error,
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
