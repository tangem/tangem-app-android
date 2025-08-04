package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
class UpdateCryptoPortfolioUseCase {

    suspend operator fun invoke(
        accountId: AccountId,
        name: AccountName? = null,
        icon: CryptoPortfolioIcon? = null,
    ): Either<Error, Account.CryptoPortfolio> = either {
        Account.CryptoPortfolio(
            accountId = accountId,
            name = name?.value ?: "Account",
            accountIcon = icon ?: CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = Random.nextInt(from = 0, until = 21),
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
            .mapLeft { Error.DataOperationFailed }
            .bind()

        // TODO: [REDACTED_JIRA]
        // Create a domain model AccountName
        // Get the current account by [accountId]
        // Create a new domain model Account from old data considering new parameters
        // Save information in local storage
        // Save to backend
    }

    sealed interface Error {

        data object DataOperationFailed : Error
    }
}