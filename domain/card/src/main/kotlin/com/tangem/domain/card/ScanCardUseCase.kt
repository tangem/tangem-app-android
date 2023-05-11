package com.tangem.domain.card

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.toEitherNel
import com.tangem.TangemSdk
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.domain.card.model.ScanCardParams
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.ChainProcessor
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import javax.inject.Inject

/**
 * Use case responsible for scanning a card and returning a [ScanResponse] object.
 * @property scanCardRepository A repository object implementing [ScanCardRepository] interface.
 * @property tangemSdk An instance of [TangemSdk] to configure the display format of the card ID.
 * @constructor Create a new instance of [ScanCardUseCase] with the given dependencies.
 */
class ScanCardUseCase @Inject internal constructor(
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
     * @param params An instance of [ScanCardParams] to configure the scan process.
     * @return A [EitherNel] object with either a non-empty list of [ScanCardException] or a [ScanResponse].
     */
    suspend operator fun invoke(params: ScanCardParams = ScanCardParams()): EitherNel<ScanCardException, ScanResponse> {
        resetCardIdDisplayFormat()
        scanChainProcessor.addChains(*params.afterScanChains)

        return scanCardRepository.scanCard(
            cardId = params.cardId,
            allowRequestAccessCodeFromRepository = params.allowRequestAccessCodeFromRepository,
        )
            .onRight { scanResponse ->
                updateCardIdDisplayFormat(scanResponse.productType)
            }
            .toEitherNel()
            .flatMap { scanResponse ->
                scanChainProcessor.launchChains(initial = scanResponse)
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