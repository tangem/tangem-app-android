package com.tangem.domain.card

import com.tangem.domain.card.repository.CardRepository

class GetCardWasScannedUseCase(private val cardRepository: CardRepository) {

    suspend operator fun invoke(cardId: String): Boolean = cardRepository.wasCardScanned(cardId)
}