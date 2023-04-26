package com.tangem.data.source.card

import arrow.core.Either
import com.tangem.data.source.card.model.Blockchain
import com.tangem.data.source.card.model.ScanException
import com.tangem.data.source.card.model.ScanResult

/**
 * Interface for a card data source that provides a method to scan a card and derive blockchains from it.
 */
interface CardDataSource {

    /**
     * Scans a card and derives blockchains from it.
     *
     * @param blockchainsToDerive a list of [Blockchain] objects representing the blockchains to derive from the card
     * @param allowsRequestAccessCodeFromStorage a boolean indicating whether the access code can be requested from
     * internal storage during the scan process
     * @return an [Either] object containing either a [ScanException] if an error occurred during the scan,
     * or a [ScanResult] object containing the scanned card data and derived blockchains
     */
    suspend fun scanCard(
        blockchainsToDerive: List<Blockchain>,
        allowsRequestAccessCodeFromStorage: Boolean,
    ): Either<ScanException, ScanResult>
}
