package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for resetting card to factory settings
 *
[REDACTED_AUTHOR]
 */
interface ResetCardUseCase {

    /** Reset card [cardId] to factory settings */
    suspend operator fun invoke(cardId: String, params: ResetCardUserCodeParams): Either<ResetCardError, Boolean>

    /** Reset backup card [cardNumber] with expected [UserWalletId] using [params] of reset card */
    suspend operator fun invoke(
        cardNumber: Int,
        params: ResetCardUserCodeParams,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Boolean>
}

data class ResetCardUserCodeParams(
    val isAccessCodeSet: Boolean,
    val isPasscodeSet: Boolean?,
)