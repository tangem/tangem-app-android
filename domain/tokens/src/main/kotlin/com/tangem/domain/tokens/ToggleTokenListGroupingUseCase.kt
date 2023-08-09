package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.*
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.error.mapper.mapToTokenListSortingError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.Network
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

    private fun Raise<TokenListSortingError>.groupTokens(tokenList: TokenList.Ungrouped): TokenList.GroupedByNetwork {
        val sortingOperations = TokenListSortingOperations(tokenList)
        val tokens = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
            sortingOperations.getTokens().bind()
        }

        val networks = getNetworks(tokens.map { it.currency.networkId }.toSet())
        return TokenList.GroupedByNetwork(
            groups = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                sortingOperations.getGroupedTokens(networks).bind()
            },
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }

    private fun Raise<TokenListSortingError>.ungroupTokens(
        tokenList: TokenList.GroupedByNetwork,
    ): TokenList.Ungrouped {
        val sortingOperations = TokenListSortingOperations(tokenList)

        return TokenList.Ungrouped(
            currencies = withError(TokenListSortingOperations.Error::mapToTokenListSortingError) {
                sortingOperations.getTokens().bind()
            },
            totalFiatBalance = tokenList.totalFiatBalance,
            sortedBy = sortingOperations.getSortType(),
        )
    }

    private fun Raise<TokenListSortingError>.getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return catch(
            block = { networksRepository.getNetworks(networksIds) },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )
    }
}