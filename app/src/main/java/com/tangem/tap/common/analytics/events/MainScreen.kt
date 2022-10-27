package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class MainScreen(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Main Screen", event, params) {

    class ScreenOpened : MainScreen("Screen opened")

    class ButtonScanCard : MainScreen("Button - Scan Card")
    class CardWasScanned : MainScreen("Card Was Scanned")
    class ButtonMyWallets : MainScreen("Button - My Wallets")

    class MainCurrencyChanged(currencyType: AnalyticsParam.CurrencyType) : MainScreen(
        event = "Main Currency Changed",
        params = mapOf("Currency Type" to currencyType.value),
    )

    sealed class NoticeRateAppButton(result: AnalyticsParam.RateApp) : MainScreen(
        event = "Notice - Rate The App Button Tapped",
        params = mapOf("Result" to result.value),
    )

    class NoticeBackupYourWalletTapped : MainScreen("Notice - Backup Your Wallet Tapped")
    class NoticeScanYourCardTapped : MainScreen("Notice - Scan Your Card Tapped")
}
