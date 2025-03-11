package com.tangem.tap.features.disclaimer

import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import java.util.Locale

fun CardDTO.createDisclaimer(): Disclaimer {
    val dataProvider = provideDisclaimerDataProvider(cardId)
    return TangemDisclaimer(dataProvider)
}

private fun provideDisclaimerDataProvider(cardId: String): DisclaimerDataProvider {
    val cardRepository = store.inject(DaggerGraphState::cardRepository)
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
