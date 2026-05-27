package com.tangem.domain.card

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.doOnFailure
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.logging.TangemLogger

/**
 * Removes saved user codes (access code and/or passcode) for a physical Tangem card
 * from the device's secure storage.
 *
 * Typically invoked after a successful factory reset of the card, so that stale codes
 * for an already-wiped card are not left on the device.
 *
 * @property tangemSdkManager Card SDK wrapper that performs the code removal operation
 */
class DeleteSavedAccessCodesUseCase(
    private val tangemSdkManager: TangemSdkManager,
) {

    /**
     * @param cardId identifier of the card whose saved codes must be removed
     * @return [Unit] on success; a Card SDK error (as [Throwable]) if removal failed
     */
    suspend operator fun invoke(cardId: String): Either<Throwable, Unit> = either {
        tangemSdkManager.deleteSavedUserCodes(cardsIds = setOf(cardId))
            .doOnFailure { error ->
                TangemLogger.e("Failed to delete saved access codes for card with id: $cardId", error)
                raise(error)
            }
    }
}