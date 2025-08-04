package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase.Error.AccountCreation
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId
import java.util.UUID

/**
[REDACTED_AUTHOR]
 */
class AddCryptoPortfolioUseCase {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        accountName: AccountName,
        icon: CryptoPortfolioIcon,
        derivationIndex: Int,
    ): Either<Error, Account.CryptoPortfolio> = either {
        Account.CryptoPortfolio(
            accountId = AccountId(userWalletId = userWalletId, value = UUID.randomUUID().toString()),
            name = accountName.value,
            accountIcon = icon,
            derivationIndex = derivationIndex,
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
            .mapLeft(::AccountCreation)
            .bind()

        // TODO: [REDACTED_JIRA]
        // Save to local store
        // Save to backend (tokens migration) – asynchronously
    }

    sealed interface Error {

        data class AccountCreation(val cause: Account.CryptoPortfolio.Error) : Error

        data class AccountListRequirementsNotMet(val cause: AccountList.Error) : Error

        data object DataOperationFailed : Error
    }
}