package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for resetting card to factory settings
 *
[REDACTED_AUTHOR]
 */
interface ResetCardUseCase {

    /** Reset card [card] to factory settings */
    suspend operator fun invoke(card: CardDTO): Either<ResetCardError, Unit>

    /** Reset backup card [cardNumber] with expected [UserWalletId] using [card] of reset card */
    suspend operator fun invoke(
        cardNumber: Int,
        card: CardDTO,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Unit>
}