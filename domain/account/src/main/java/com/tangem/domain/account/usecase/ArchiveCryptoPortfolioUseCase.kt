package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.account.AccountId

/**
[REDACTED_AUTHOR]
 */
class ArchiveCryptoPortfolioUseCase {

    suspend operator fun invoke(accountId: AccountId): Either<Error, Unit> = either {
        // TODO: [REDACTED_JIRA]
        // Remove the account from the list of active accounts
        // Add the account to the list of archived accounts
        // Save to backend
    }

    sealed interface Error {
        data object DataOperationFailed : Error
    }
}