package com.tangem.domain.card

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.flatMap
import com.tangem.TangemSdk
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ChainProcessor
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse

/**
 * Use case responsible for scanning a card and returning a [ScanResponse] object.
 * @property scanCardRepository A repository object implementing [ScanCardRepository] interface.
 * @property tangemSdk An instance of [TangemSdk] to configure the display format of the card ID.
 * @constructor Create a new instance of [ScanCardUseCase] with the given dependencies.
 */
class ScanCardUseCase(
    private val scanCardRepository: ScanCardRepository,
    private val tangemSdk: TangemSdk,
) {

    /**
     * A [ChainProcessor] object to launch the after-scan chains.
     */
    private val scanChainProcessor by lazy {
        ChainProcessor<ScanCardException.ChainException, ScanResponse>()
    }

    /**
     * Scan a card and return a [ScanResponse] object.
     * @param cardId an optional card ID to scan. If null, can scan any card present.
     * Defaults to null.
     * @param allowRequestAccessCodeFromStorage whether to prompt the user for an access code if needed.
     * Defaults to false.
     * @param afterScanChains An array of chains that should be executed after a successful card scan operation.
     * Defaults to an empty array.
     * @return A [EitherNel] object with either a non-empty list of [ScanCardException] or a [ScanResponse].
     */
    suspend operator fun invoke(
        cardId: String? = null,
        allowRequestAccessCodeFromStorage: Boolean = false,
        afterScanChains: Array<Chain<ScanCardException.ChainException, ScanResponse>> = emptyArray(),
    ): Either<ScanCardException, ScanResponse> {
        resetCardIdDisplayFormat()
        scanChainProcessor.addChains(afterScanChains)

        return scanCardRepository.scanCard(
            cardId = cardId,
            allowRequestAccessCodeFromStorage = allowRequestAccessCodeFromStorage,
        )
            .onRight { scanResponse ->
                updateCardIdDisplayFormat(scanResponse.productType)
            }
            .flatMap { response ->
                scanChainProcessor.launchChains(initial = response)
            }
    }

    /**
     * Reset the card ID display format to [CardIdDisplayFormat.Full].
     */
    private fun resetCardIdDisplayFormat() {
        tangemSdk.config.cardIdDisplayFormat = CardIdDisplayFormat.Full
    }

    /**
     * Update the card ID display format according to the [ProductType] of the scanned card.
     * @param productType The [ProductType] of the scanned card.
     */
    private fun updateCardIdDisplayFormat(productType: ProductType) {
        tangemSdk.config.cardIdDisplayFormat = when (productType) {
            ProductType.Twins -> CardIdDisplayFormat.LastLuhn(numbers = 4)
            ProductType.SaltPay -> CardIdDisplayFormat.None
            ProductType.Note,
            ProductType.Wallet,
            ProductType.Start2Coin,
            -> CardIdDisplayFormat.Full
        }
    }
}