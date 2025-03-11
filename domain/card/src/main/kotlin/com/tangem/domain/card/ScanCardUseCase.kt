package com.tangem.domain.card

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ChainProcessor
import com.tangem.domain.models.scan.ScanResponse

/**
 * Use case responsible for scanning a card and returning a [ScanResponse] object.
 */
class ScanCardUseCase(
    private val scanCardRepository: ScanCardRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
) {

    /** A [ChainProcessor] object to launch the after-scan chains */
    private val scanChainProcessor = ChainProcessor<ScanCardException, ScanResponse>()

    /**
     * Scan a card.
     *
     * @param cardId An optional card ID to scan. If `null`, can scan any card present
     * @param allowRequestAccessCodeFromStorage Whether to prompt the user for an access code if needed
     * @param afterScanChains A list of chains that should be executed after a card scan
     * operation. Defaults to an empty array.
     *
     * @return [Either] object with either a [ScanCardException] or a [ScanResponse] object.
     */
    suspend operator fun invoke(
        cardId: String? = null,
        allowRequestAccessCodeFromStorage: Boolean = false,
        afterScanChains: List<Chain<ScanCardException, ScanResponse>> = emptyList(),
    ): Either<ScanCardException, ScanResponse> = either {
        cardSdkConfigRepository.resetCardIdDisplayFormat()
        scanChainProcessor.setChains(afterScanChains)

        val maybeScanResponse = scanCard(cardId, allowRequestAccessCodeFromStorage)
        val scanResponse = scanChainProcessor.launchChains(maybeScanResponse).bind()

        cardSdkConfigRepository.updateCardIdDisplayFormat(scanResponse.productType)

        scanResponse
    }

    private suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromStorage: Boolean,
    ): Either<ScanCardException, ScanResponse> {
        return Either.catch { scanCardRepository.scanCard(cardId, allowRequestAccessCodeFromStorage) }
            .mapLeft { e ->
                e as? ScanCardException ?: ScanCardException.UnknownException(e)
            }
    }
}