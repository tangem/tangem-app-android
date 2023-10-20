package com.tangem.domain.card

import com.tangem.domain.card.repository.CardRepository
import kotlinx.coroutines.flow.Flow

class WasCardScannedUseCase(private val cardRepository: CardRepository) {

    operator fun invoke(cardId: String): Flow<Boolean> = cardRepository.wasCardScanned(cardId)
}