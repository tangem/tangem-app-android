package com.tangem.tap.features.disclaimer

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.models.scan.CardDTO
import com.tangem.data.source.preferences.storage.DisclaimerPrefStorage
import com.tangem.tap.preferencesStorage
import java.util.Locale

/**
[REDACTED_AUTHOR]
 */
enum class DisclaimerType {
    Tangem,
    Start2Coin,
    ;

    companion object {
        fun get(cardDTO: CardDTO): DisclaimerType {
            return when {
                cardDTO.isStart2Coin -> Start2Coin
                else -> Tangem
            }
        }
    }
}

fun DisclaimerType.createDisclaimer(cardDTO: CardDTO): Disclaimer {
    val dataProvider = provideDisclaimerDataProvider(cardDTO.cardId)
    return when (this) {
        DisclaimerType.Tangem -> TangemDisclaimer(dataProvider)
        DisclaimerType.Start2Coin -> Start2CoinDisclaimer(dataProvider)
    }
}

fun CardDTO.createDisclaimer(): Disclaimer = DisclaimerType.get(this).createDisclaimer(this)

private fun provideDisclaimerDataProvider(cardId: String): DisclaimerDataProvider {
    return object : DisclaimerDataProvider {
        override fun getLanguage(): String = Locale.getDefault().language
        override fun getCardId(): String = cardId
        override fun storage(): DisclaimerPrefStorage = preferencesStorage.disclaimerPrefStorage
    }
}