package com.tangem.domain.card.repository

import arrow.core.Either
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.models.scan.ScanResponse

/**
 * A repository for scanning a card.
 */
interface ScanCardRepository {

    /**
     * Scans the card with the given [cardId] and returns a [ScanResponse]. If [allowRequestAccessCodeFromRepository]
     * is true, the repository may prompt the user for an access code.
     *
     * @param cardId an optional card ID to scan. If null, the repository should scan any card present.
     * @param allowRequestAccessCodeFromRepository whether to prompt the user for an access code if needed.
     * @return an [Either] that contains a [ScanCardException] in case of an error or a [ScanResponse].
     */
    suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): Either<ScanCardException, ScanResponse>
}
