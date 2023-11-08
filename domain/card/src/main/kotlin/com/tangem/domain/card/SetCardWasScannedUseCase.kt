package com.tangem.domain.card

import com.tangem.domain.card.repository.CardRepository

class SetCardWasScannedUseCase(private val cardRepository: CardRepository) {

    suspend operator fun invoke(cardId: String) = cardRepository.setCardWasScanned(cardId)
}