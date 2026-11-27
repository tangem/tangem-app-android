package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent

class GetPolymarketSoccerEventUseCase(private val polymarketRepository: PolymarketRepository) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        eventId: String,
    ): Either<PolymarketEventError, PolymarketSoccerEvent> =
        polymarketRepository.getSoccerEvent(userWalletId = userWalletId, eventId = eventId)
}