package com.tangem.tap.features.disclaimer

import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.tap.persistence.DisclaimerPrefStorage
import com.tangem.tap.preferencesStorage
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
enum class DisclaimerType {
    Tangem,
    SaltPay,
    Start2Coin;

    companion object {
        fun get(cardDTO: CardDTO): DisclaimerType {
            return when {
                cardDTO.isSaltPay -> SaltPay
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
        DisclaimerType.SaltPay -> SaltPayDisclaimer(dataProvider)
        DisclaimerType.Start2Coin -> Start2CoinDisclaimer(dataProvider)
    }
}

private fun provideDisclaimerDataProvider(cardId: String): DisclaimerDataProvider {
    return object : DisclaimerDataProvider {
        override fun getLanguage(): String = Locale.getDefault().language
        override fun getCardId(): String = cardId
        override fun storage(): DisclaimerPrefStorage = preferencesStorage.disclaimerPrefStorage
    }
}
