package com.tangem.tap.features.disclaimer

import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.CardDTO
import java.util.Locale

fun CardDTO.createDisclaimer(cardRepository: CardRepository): Disclaimer {
    val dataProvider = provideDisclaimerDataProvider(cardId, cardRepository)
    return TangemDisclaimer(dataProvider)
}

private fun provideDisclaimerDataProvider(cardId: String, cardRepository: CardRepository): DisclaimerDataProvider {
    return object : DisclaimerDataProvider {
        override fun getLanguage(): String = Locale.getDefault().language
        override fun getCardId(): String = cardId

        override suspend fun accept() {
            cardRepository.acceptTangemTOS()
        }

        override suspend fun isAccepted(): Boolean {
            return cardRepository.isTangemTOSAccepted()
        }
    }
}