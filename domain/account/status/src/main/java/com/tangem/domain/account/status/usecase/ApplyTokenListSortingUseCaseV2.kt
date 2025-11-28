package com.tangem.domain.account.status.usecase

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.model.AccountCryptoCurrencies
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

private typealias SortingErrorByAccountId = MutableMap<AccountId, TokenListSortingError>

/**
 * Use case to apply token list sorting and grouping preferences for accounts.
 *
 * @property accountsCRUDRepository Repository for CRUD operations on accounts.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 */
class ApplyTokenListSortingUseCaseV2(
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Applies the token list sorting and grouping preferences for the given accounts.
     *
     * @param sortedTokensIdsByAccount A map of accounts to their sorted token IDs.
     * @param isGroupedByNetwork Boolean indicating if tokens should be grouped by network.
     * @param isSortedByBalance Boolean indicating if tokens should be sorted by balance.
     * @return Either a [TokenListSortingError] or Unit on success.
     */
    suspend operator fun invoke(
        sortedTokensIdsByAccount: AccountCryptoCurrencies,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokenListSortingError, Unit> = eitherOn(dispatchers.default) {
        ensure(sortedTokensIdsByAccount.isNotEmpty()) {
            raise(TokenListSortingError.TokenListIsEmpty)
        }

        val userWalletId = sortedTokensIdsByAccount.keys.first().userWalletId

        val accountList = getAccountList(userWalletId)

        val maybeSortedAccountList = sortAccountList(
            accountList = accountList,
            sortedTokensIdsByAccount = sortedTokensIdsByAccount,
            isSortedByBalance = isSortedByBalance,
            isGroupedByNetwork = isGroupedByNetwork,
        )

        val sortedAccountList = maybeSortedAccountList.getOrNull()
        val isSortingTypeChanged = accountList.isSortedByBalance() != isSortedByBalance
        val isGroupingTypeChanged = accountList.isGroupedByNetwork() != isGroupedByNetwork

        if (sortedAccountList != null || isSortingTypeChanged || isGroupingTypeChanged) {
            applySorting(sortedAccountList ?: accountList)
        }

        maybeSortedAccountList.toEitherNeg()
            .onLeft { errorByAccountId ->
                Timber.e(
                    """
                    Unable to sort tokens for accounts: ${
                        errorByAccountId.entries.joinToString { "${it.key.value}: ${it.value}" }
                    }
                    """.trimIndent(),
                )

                raise(TokenListSortingError.UnableToSortAccounts(errorByAccountId.keys.map { it.value }))
            }
    }

    private suspend fun Raise<TokenListSortingError>.getAccountList(userWalletId: UserWalletId): AccountList {
        return catchOrDataError {
            accountsCRUDRepository.getAccountListSync(userWalletId = userWalletId).getOrElse {
                throw IllegalStateException("Account list not found for wallet $userWalletId")
            }
        }
    }

    private fun AccountList.isSortedByBalance(): Boolean = sortType == TokensSortType.BALANCE

    private fun AccountList.isGroupedByNetwork(): Boolean = groupType == TokensGroupType.NETWORK

    private fun Raise<TokenListSortingError>.sortAccountList(
        accountList: AccountList,
        sortedTokensIdsByAccount: AccountCryptoCurrencies,
        isSortedByBalance: Boolean,
        isGroupedByNetwork: Boolean,
    ): Ior<SortingErrorByAccountId, AccountList> {
        val errors = mutableMapOf<AccountId, TokenListSortingError>()

        val updatedAccountList = AccountList(
            userWalletId = accountList.userWalletId,
            accounts = accountList.accounts.sortTokens(sortedTokensIdsByAccount, errors),
            totalAccounts = accountList.totalAccounts,
            sortType = if (isSortedByBalance) TokensSortType.BALANCE else TokensSortType.NONE,
            groupType = if (isGroupedByNetwork) TokensGroupType.NETWORK else TokensGroupType.NONE,
        ).getOrElse {
            raise(TokenListSortingError.UnableToSortTokenList)
        }

        return if (errors.isEmpty()) {
            updatedAccountList.rightIor()
        } else if (errors.size == sortedTokensIdsByAccount.keys.size) {
            errors.leftIor()
        } else {
            (errors to updatedAccountList).bothIor()
        }
    }

    private fun List<Account>.sortTokens(
        sortedTokensIdsByAccount: AccountCryptoCurrencies,
        errors: MutableMap<AccountId, TokenListSortingError>,
    ): List<Account> {
        return map { account ->
            if (account !is Account.CryptoPortfolio) return@map account

            val sortedCurrencies = sortedTokensIdsByAccount[account] ?: return@map account

            val accountCurrencies = account.cryptoCurrencies.toList()
                .sortTokens(sortedCurrencies = sortedCurrencies)
                .getOrElse {
                    errors[account.accountId] = it
                    return@map account
                }
                .toSet()

            account.copy(cryptoCurrencies = accountCurrencies)
        }
    }

    private fun List<CryptoCurrency>.sortTokens(
        sortedCurrencies: List<CryptoCurrency>,
    ): Either<TokenListSortingError, List<CryptoCurrency>> = either {
        val nonEmptySortedTokensIds = ensureNotNull(sortedCurrencies.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }

        val sortedTokens = sortedMapOf<Int, CryptoCurrency>()

        distinct().forEach { currency ->
            val index = nonEmptySortedTokensIds.indexOfFirst { sortedCurrency ->
                sortedCurrency.id == currency.id
            }

            if (index >= 0) {
                sortedTokens[index] = currency
            } else {
                raise(TokenListSortingError.UnableToSortTokenList)
            }
        }

        ensureNotNull(sortedTokens.values.toNonEmptyListOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.applySorting(accountList: AccountList) {
        coroutineScope {
            val results = awaitAll(
                async {
                    Either.catch { accountsCRUDRepository.saveAccountsLocally(accountList) }
                },
                async {
                    Either.catch { accountsCRUDRepository.syncTokens(accountList.userWalletId) }
                },
            )

            ensure(results.none { it.isLeft() }) {
                val message = results.mapNotNull { it.leftOrNull() }.joinToString()
                raise(TokenListSortingError.DataError(IllegalStateException(message)))
            }
        }
    }

    private fun Ior<SortingErrorByAccountId, AccountList>.toEitherNeg(): Either<SortingErrorByAccountId, AccountList> {
        return when (this) {
            is Ior.Left -> this.value.left()
            is Ior.Right -> this.value.right()
            is Ior.Both -> this.leftValue.left()
        }
    }

    private suspend fun <A> Raise<TokenListSortingError>.catchOrDataError(block: suspend () -> A): A {
        return catch(
            block = { block() },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )
    }
}