package com.tangem.domain.card.repository

import arrow.core.Either
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.models.scan.ScanResponse

/**
 * A repository for scanning a card.
 */
interface ScanCardRepository {

    /**
     * Scans the card with the given [cardId] and returns a [ScanResponse]
     *
     * @param cardId an optional card ID to scan. If null, the repository should scan any card present.
     * @param allowRequestAccessCodeFromStorage whether the access code can be requested from
     * internal storage during the scan process
     * @return an [Either] that contains a [ScanCardException] in case of an error or a [ScanResponse].
     */
    suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromStorage: Boolean,
    ): Either<ScanCardException, ScanResponse>
}