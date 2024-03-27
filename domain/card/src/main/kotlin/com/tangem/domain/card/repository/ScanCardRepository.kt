package com.tangem.domain.card.repository

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
     * @return a [ScanResponse] object with the result of the scan.
     * @throws [ScanCardException] if the scan process fails.
     */
    suspend fun scanCard(cardId: String?, allowRequestAccessCodeFromStorage: Boolean): ScanResponse
}