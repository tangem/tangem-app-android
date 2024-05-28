package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.scan.CardDTO

/**
 * Use case for resetting card to factory settings
 *
* [REDACTED_AUTHOR]
 */
interface ResetCardUseCase {

    suspend operator fun invoke(card: CardDTO): Either<ResetCardError, Unit>
}
