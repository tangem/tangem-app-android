package com.tangem.tap.domain.termsOfUse

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class TermsOfUse {
    val locale: Locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)

    fun getUrl(cardId: String): String? {
        val terms = CardTermsFactory.create(cardId) ?: return null

        return terms.getUrl(locale.country)
    }
}

class CardTermsOfUse(
        private val urlsByRegion: Map<String, String>,
        private val defaultRegion: String,
) {

    fun getUrl(region: String): String? {
        return urlsByRegion[region.toLowerCase()] ?: urlsByRegion[defaultRegion]
    }
}


class CardTermsFactory {
    companion object {

        fun create(cardId: String): CardTermsOfUse? {
            val batch = cardId.substring(0..3)

            return when (batch) {
                "CB05" -> {
                    CardTermsOfUse(
                            mapOf(
                                    "ru" to "https://yandex.ru/",
                                    "en" to "https://www.google.com/",
                                    "fr" to "https://www.qwant.com",
                            ),
                            "ru"
                    )
                }
                else -> null
            }
        }
    }
}