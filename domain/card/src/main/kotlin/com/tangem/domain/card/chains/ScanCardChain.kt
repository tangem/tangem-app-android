package com.tangem.domain.card.chains

import com.tangem.TangemSdk
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.domain.card.ScanCardChain
import com.tangem.domain.card.ScanCardChainResult
import com.tangem.domain.card.model.ScanCardParams
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.core.chain.toChainResult

internal class ScanCardChain(
    private val params: ScanCardParams,
    private val tangemSdk: TangemSdk,
    private val scanCardRepository: ScanCardRepository,
) : ScanCardChain {
    override suspend fun invoke(previousChainResult: ScanCardChainResult): ScanCardChainResult {
        tangemSdk.config.cardIdDisplayFormat = CardIdDisplayFormat.Full

        return scanCardRepository.scanCard(
            cardId = params.cardId,
            message = params.message,
            allowRequestAccessCodeFromRepository = params.allowRequestAccessCodeFromRepository,
        )
            .onSuccess {
                // TODO: Update card ID display format with card type
                tangemSdk.config.cardIdDisplayFormat = CardIdDisplayFormat.Full
            }
            .toChainResult()
    }
}
