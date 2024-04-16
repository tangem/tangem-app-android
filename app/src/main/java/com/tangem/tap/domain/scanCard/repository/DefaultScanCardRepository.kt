package com.tangem.tap.domain.scanCard.repository

import com.tangem.common.CompletionResult
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.utils.ScanCardExceptionConverter
// [REDACTED_TODO_COMMENT]
internal class DefaultScanCardRepository(
    private val tangemSdkManager: TangemSdkManager,
) : ScanCardRepository {

    private val exceptionConverter = ScanCardExceptionConverter()

    override suspend fun scanCard(cardId: String?, allowRequestAccessCodeFromStorage: Boolean): ScanResponse {
        return when (
            val result = tangemSdkManager.scanProduct(
                cardId = cardId,
                allowsRequestAccessCodeFromRepository = allowRequestAccessCodeFromStorage,
            )
        ) {
            is CompletionResult.Success -> result.data
            is CompletionResult.Failure -> throw exceptionConverter.convert(result.error)
        }
    }
}
