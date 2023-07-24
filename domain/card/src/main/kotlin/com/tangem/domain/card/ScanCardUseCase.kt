package com.tangem.domain.card

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.flatMap
import com.tangem.TangemSdk
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ChainProcessor
import com.tangem.domain.models.scan.ScanResponse

/**
 * Use case responsible for scanning a card and returning a [ScanResponse] object
 *
 * @property scanCardRepository  a repository object implementing [ScanCardRepository] interface
 * @property cardSdkConfigRepository an instance of [TangemSdk] to configure the display format of the card ID
 *
 * @constructor create a new instance of [ScanCardUseCase] with the given dependencies
 */
class ScanCardUseCase(
    private val scanCardRepository: ScanCardRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
) {

    /** A [ChainProcessor] object to launch the after-scan chains */
    private val scanChainProcessor by lazy {
        ChainProcessor<ScanCardException.ChainException, ScanResponse>()
    }

    /**
     * Scan a card and return a [ScanResponse] object.
     *
     * @param cardId                            an optional card ID to scan. If null, can scan any card present
     * @param allowRequestAccessCodeFromStorage whether to prompt the user for an access code if needed
     * @param afterScanChains                   a list of chains that should be executed after a successful card scan
     * operation. Defaults to an empty array
     *
     * @return a [EitherNel] object with either a non-empty list of [ScanCardException] or a [ScanResponse]
     */
    suspend operator fun invoke(
        cardId: String? = null,
        allowRequestAccessCodeFromStorage: Boolean = false,
        afterScanChains: List<Chain<ScanCardException.ChainException, ScanResponse>> = emptyList(),
    ): Either<ScanCardException, ScanResponse> {
        cardSdkConfigRepository.resetCardIdDisplayFormat()
        scanChainProcessor.addChains(afterScanChains)

        return scanCardRepository.scanCard(
            cardId = cardId,
            allowRequestAccessCodeFromStorage = allowRequestAccessCodeFromStorage,
        )
            .onRight { scanResponse ->
                cardSdkConfigRepository.updateCardIdDisplayFormat(scanResponse.productType)
            }
            .flatMap { response ->
                scanChainProcessor.launchChains(initial = response)
            }
    }
}
