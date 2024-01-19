package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class Token(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    sealed class Receive(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Receive", event, params) {

        class ScreenOpened : Receive("Receive Screen Opened")
        class ButtonCopyAddress : Receive("Button - Copy Address")
        class ButtonShareAddress : Receive("Button - Share Address")
    }

    sealed class Send(
        event: String,
        params: Map<String, String> = mapOf(),
        error: Throwable? = null,
    ) : Token("Token / Send", event, params, error) {

        class ScreenOpened : Send(event = "Send Screen Opened")
        class ButtonPaste : Send(event = "Button - Paste")
        class ButtonQRCode : Send(event = "Button - QR Code")
        class ButtonSwapCurrency : Send(event = "Button - Swap Currency")

        class AddressEntered(sourceType: SourceType, validationResult: ValidationResult) : Send(
            event = "Address Entered",
            params = mapOf(
                "Source" to sourceType.name,
                "Validation" to validationResult.name,
            ),
        ) {
            enum class SourceType {
                QRCode, PasteButton, PastePopup
            }

            enum class ValidationResult {
                Success, Fail
            }
        }

        class SelectedCurrency(currency: CurrencyType) : Send(
            event = "Selected Currency",
            params = mapOf("Type" to currency.value),
        ) {

            enum class CurrencyType(val value: String) {
                Token(value = "Token"), AppCurrency(value = "App Currency")
            }
        }
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