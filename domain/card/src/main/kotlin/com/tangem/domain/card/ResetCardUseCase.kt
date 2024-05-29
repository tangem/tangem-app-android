package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.scan.CardDTO

/**
 * Use case for resetting card to factory settings
 *
 * @author Andrew Khokhlov on 27/05/2024
 */
interface ResetCardUseCase {

    suspend operator fun invoke(card: CardDTO): Either<ResetCardError, Unit>
}
