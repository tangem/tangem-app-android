package com.tangem.tap.domain.termsOfUse

import android.content.res.Resources
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import com.tangem.domain.common.CardDTO
import java.util.*

/**
 * Created by Anton Zhilenkov on 19/02/2021.
 */
class CardTou {
    private val locale: Locale =
        ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)!!

    fun getUrl(card: CardDTO): Uri? {
        val issuerName = card.issuer.name
        if (issuerName.lowercase(Locale.getDefault()) != "start2coin") return null

        val baseUrl = "https://app.tangem.com/tou/"
        val regionCode = regionCode(card.cardId) ?: "fr"
        val filename = filename(locale.language, regionCode)
        return Uri.parse(baseUrl + filename)
    }

    private fun filename(languageCode: String, regionCode: String): String {
        return when {
            languageCode == "fr" && regionCode == "ch" -> "Start2Coin-fr-ch-tangem.pdf"
            languageCode == "de" && regionCode == "ch" -> "Start2Coin-de-ch-tangem.pdf"
            languageCode == "en" && regionCode == "ch" -> "Start2Coin-en-ch-tangem.pdf"
            languageCode == "it" && regionCode == "ch" -> "Start2Coin-it-ch-tangem.pdf"
            languageCode == "fr" && regionCode == "fr" -> "Start2Coin-fr-fr-atangem.pdf"
            languageCode == "de" && regionCode == "at" -> "Start2Coin-de-at-tangem.pdf"
            regionCode == "fr" -> "Start2Coin-fr-fr-atangem.pdf"
            regionCode == "ch" -> "Start2Coin-en-ch-tangem.pdf"
            regionCode == "at" -> "Start2Coin-de-at-tangem.pdf"
            else -> "Start2Coin-fr-fr-atangem.pdf"
        }
    }

    private fun regionCode(cardId: String): String? = when (cardId[1]) {
        '0' -> "fr"
        '1' -> "ch"
        '2' -> "at"
        else -> null
    }
}
