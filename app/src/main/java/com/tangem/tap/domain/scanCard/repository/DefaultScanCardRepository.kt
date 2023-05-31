package com.tangem.tap.domain.scanCard.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.repository.ScanCardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.utils.ScanCardExceptionConverter
import com.tangem.tap.domain.tokens.UserTokensRepository

internal class DefaultScanCardRepository(
    // FIXME: The repository should not depend on another repository.
    //  But now we need to provide loadBlockchainsToDerive() to ScanProductTask and it's hard to move this method from
    //  UserTokensRepository.
    private val userTokensRepository: UserTokensRepository,
    private val tangemSdkManager: TangemSdkManager,
) : ScanCardRepository {

    private val exceptionConverter = ScanCardExceptionConverter()

    override suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromStorage: Boolean,
    ): Either<ScanCardException, ScanResponse> = either {
        when (
            val result = tangemSdkManager.scanProduct(
                cardId = cardId,
                userTokensRepository = userTokensRepository,
                allowsRequestAccessCodeFromRepository = allowRequestAccessCodeFromStorage,
            )
        ) {
            is CompletionResult.Success -> result.data
            is CompletionResult.Failure -> raise(exceptionConverter.convert(result.error))
        }
    }
}