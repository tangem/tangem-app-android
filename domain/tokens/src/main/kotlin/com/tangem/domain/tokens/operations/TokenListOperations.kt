package com.tangem.domain.tokens.operations

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class TokenListOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val userWalletId: UserWalletId,
    private val tokens: List<CryptoCurrencyStatus>,
) {

    fun getTokenListFlow(): Flow<Either<Error, TokenList>> {
        return combine(
            flow = getIsGrouped(),
            flow2 = getIsSortedByBalance(),
        ) { isGrouped, isSortedByBalance ->
            either {
                createTokenList(isGrouped = isGrouped.bind(), isSortedByBalance = isSortedByBalance.bind())
            }
        }
    }

    private fun createTokenList(isGrouped: Boolean, isSortedByBalance: Boolean): TokenList {
        val nonEmptyCurrencies = tokens.toNonEmptyListOrNull() ?: return TokenList.Empty

        return TokenListFactory.create(
            statuses = nonEmptyCurrencies,
            groupType = if (isGrouped) TokensGroupType.NETWORK else TokensGroupType.NONE,
            sortType = if (isSortedByBalance) TokensSortType.BALANCE else TokensSortType.NONE,
        )
    }

    private fun getIsGrouped(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensGrouped(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
            .cancellable()
    }

    private fun getIsSortedByBalance(): Flow<Either<Error, Boolean>> {
        return currenciesRepository.isTokensSortedByBalance(userWalletId)
            .map<Boolean, Either<Error, Boolean>> { it.right() }
            .catch { emit(Error.DataError(it).left()) }
            .onEmpty { emit(value = false.right()) }
            .cancellable()
    }

    sealed class Error {

        data class UnableToSortTokenList(val unsortedTokenList: TokenList.Ungrouped) : Error()

        data class DataError(val cause: Throwable) : Error()
    }
}