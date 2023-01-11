package com.tangem.tap.features.disclaimer

import android.net.Uri

/**
[REDACTED_AUTHOR]
 */
interface Disclaimer {
    fun type(): DisclaimerType
    fun getUri(): Uri
    fun accept()
    fun isAccepted(): Boolean
}

abstract class BaseDisclaimer(
    protected val dataProvider: DisclaimerDataProvider,
) : Disclaimer {

    val baseUrl = "https://tangem.com"

    override fun accept() {
        dataProvider.storage().accept(getPreferenceKey())
    }

    override fun isAccepted(): Boolean = dataProvider.storage().isAccepted(getPreferenceKey())

    protected open fun getPreferenceKey(): String = type().name
}

class DummyDisclaimer : Disclaimer {
    override fun type(): DisclaimerType = DisclaimerType.Tangem
    override fun getUri(): Uri = Uri.parse("https://tangem.com/tangem_tos.html")
    override fun accept() {}
    override fun isAccepted(): Boolean = false
}

class TangemDisclaimer(dataProvider: DisclaimerDataProvider) : BaseDisclaimer(dataProvider) {
    override fun type(): DisclaimerType = DisclaimerType.Tangem
    override fun getUri(): Uri = Uri.parse("$baseUrl/tangem_tos.html")
    override fun getPreferenceKey(): String = "tangem_tos_accepted"
}

class SaltPayDisclaimer(dataProvider: DisclaimerDataProvider) : BaseDisclaimer(dataProvider) {
    override fun type(): DisclaimerType = DisclaimerType.SaltPay
    override fun getUri(): Uri = Uri.parse("$baseUrl/soltpay_tos.html")
    override fun getPreferenceKey(): String = "saltPay_tos_accepted"
}

class Start2CoinDisclaimer(dataProvider: DisclaimerDataProvider) : BaseDisclaimer(dataProvider) {
    override fun type(): DisclaimerType = DisclaimerType.Start2Coin
    override fun getUri(): Uri = Uri.parse("$baseUrl/" + filename(dataProvider.getLanguage(), getRegion()))
    override fun getPreferenceKey(): String = "start2Coin_tos_accepted_${getRegion()}"

    private fun filename(languageCode: String, regionCode: String?): String {
        return when {
            languageCode == "fr" && regionCode == "ch" -> "Start2Coin-fr-ch-tangem.html"
            languageCode == "de" && regionCode == "ch" -> "Start2Coin-de-ch-tangem.html"
            languageCode == "en" && regionCode == "ch" -> "Start2Coin-en-ch-tangem.html"
            languageCode == "it" && regionCode == "ch" -> "Start2Coin-it-ch-tangem.html"
            languageCode == "fr" && regionCode == "fr" -> "Start2Coin-fr-fr-atangem.html"
            languageCode == "de" && regionCode == "at" -> "Start2Coin-de-at-tangem.html"
            regionCode == "fr" -> "Start2Coin-fr-fr-atangem.html"
            regionCode == "ch" -> "Start2Coin-en-ch-tangem.html"
            regionCode == "at" -> "Start2Coin-de-at-tangem.html"
            else -> "Start2Coin-fr-fr-atangem.html"
        }
    }

    private fun getRegion(): String? {
        val cardId = dataProvider.getCardId()
        if (cardId.isEmpty()) return null

        return when (cardId[1]) {
            '0' -> "fr"
            '1' -> "ch"
            '2' -> "at"
            else -> null
        }
    }
}