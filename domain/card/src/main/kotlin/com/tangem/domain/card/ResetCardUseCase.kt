package com.tangem.domain.card

import arrow.core.Either
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.wallet.UserWallet

/**
 * Use case for resetting card to factory settings
 *
[REDACTED_AUTHOR]
 */
interface ResetCardUseCase {

    /** Reset card [cardId] to factory settings */
    suspend operator fun invoke(cardId: String, params: ResetCardUserCodeParams): Either<ResetCardError, Boolean>

    /** Reset backup card [cardNumber] of [userWallet] using [params] of reset card */
    suspend operator fun invoke(
        cardNumber: Int,
        params: ResetCardUserCodeParams,
        userWallet: UserWallet.Cold,
    ): Either<ResetCardError, Boolean>
}

data class ResetCardUserCodeParams(
    val isAccessCodeSet: Boolean,
    val isPasscodeSet: Boolean?,
)