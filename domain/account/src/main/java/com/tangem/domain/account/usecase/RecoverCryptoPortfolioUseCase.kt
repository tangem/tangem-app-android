package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId

/**
[REDACTED_AUTHOR]
 */
class RecoverCryptoPortfolioUseCase {

    suspend operator fun invoke(accountId: AccountId): Either<Error, Account.CryptoPortfolio> = either {
        raise(Error.DataOperationFailed)

        // TODO: [REDACTED_JIRA]
        // Remove the account from the list of archived accounts
        // Add the account to the list of active accounts
        // Save to backend
    }

    sealed interface Error {
        data object DataOperationFailed : Error
    }
}