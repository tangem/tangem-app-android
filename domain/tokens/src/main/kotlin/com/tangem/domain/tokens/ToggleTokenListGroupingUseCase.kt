package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.error.mapper.mapToTokenListSortingError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.operations.TokenListSortingOperations
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ToggleTokenListGroupingUseCase(
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(tokenList: TokenList): Either<TokenListSortingError, TokenList> {
        return withContext(dispatchers.default) {
            either {
                ensure(tokenList.totalFiatBalance !is TokenList.FiatBalance.Loading) {
                    TokenListSortingError.TokenListIsLoading
                }

                when (tokenList) {
                    is TokenList.GroupedByNetwork -> ungroupTokens(tokenList)
                    is TokenList.Ungrouped -> groupTokens(tokenList)
                    is TokenList.NotInitialized -> raise(TokenListSortingError.TokenListIsEmpty)
                }
            }
        }
    }

    private suspend fun Raise<TokenListSortingError>.groupTokens(
        tokenList: TokenList.Ungrouped,
    ): TokenList.GroupedByNetwork {
        val sortingOperations = getSortingOperations(tokenList)
        val tokens = sortingOperations.getTokens()

        val networks = getNetworks(tokens.map { it.currency.networkId }.toSet())
        return TokenList.GroupedByNetwork(
            groups = sortingOperations.getGroupedTokens(networks),
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }

    private suspend fun Raise<TokenListSortingError>.ungroupTokens(
        tokenList: TokenList.GroupedByNetwork,
    ): TokenList.Ungrouped {
        val sortingOperations = getSortingOperations(tokenList)

        return TokenList.Ungrouped(
            currencies = sortingOperations.getTokens(),
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }

    private fun Raise<TokenListSortingError>.getSortingOperations(tokenList: TokenList): TokenListSortingOperations<*> {
        return TokenListSortingOperations(
            tokenList = tokenList,
            dispatchers = dispatchers,
            raise = this,
            transformError = TokenListSortingOperations.Error::mapToTokenListSortingError,
        )
    }

    private suspend fun Raise<TokenListSortingError>.getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return withContext(dispatchers.io) {
            catch(
                block = { networksRepository.getNetworks(networksIds) },
                catch = { raise(TokenListSortingError.DataError(it)) },
            )
        }
    }
}