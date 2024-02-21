package com.tangem.tap.features.disclaimer

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
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
    val dataProvider = provideDisclaimerDataProvider(cardDTO.cardId, this)
    return when (this) {
        DisclaimerType.Tangem -> TangemDisclaimer(dataProvider)
        DisclaimerType.Start2Coin -> Start2CoinDisclaimer(dataProvider)
    }
}

fun CardDTO.createDisclaimer(): Disclaimer = DisclaimerType.get(this).createDisclaimer(this)

private fun provideDisclaimerDataProvider(cardId: String, disclaimerType: DisclaimerType): DisclaimerDataProvider {
    val cardRepository = store.inject(DaggerGraphState::cardRepository)
    return object : DisclaimerDataProvider {
        override fun getLanguage(): String = Locale.getDefault().language
        override fun getCardId(): String = cardId

        override suspend fun accept() {
            when (disclaimerType) {
                DisclaimerType.Tangem -> cardRepository.acceptTangemTOS()
                DisclaimerType.Start2Coin -> cardRepository.acceptStart2CoinTOS(cardId)
            }
        }

        override suspend fun isAccepted(): Boolean {
            return when (disclaimerType) {
                DisclaimerType.Tangem -> cardRepository.isTangemTOSAccepted()
                DisclaimerType.Start2Coin -> cardRepository.isStart2CoinTOSAccepted(cardId)
            }
        }
    }
}