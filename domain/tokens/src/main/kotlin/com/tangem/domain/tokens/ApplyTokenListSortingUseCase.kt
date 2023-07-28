package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ApplyTokenListSortingUseCase(
    private val tokensRepository: TokensRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Pair<Network.ID, Token.ID>>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokenListSortingError, Unit> {
        return withContext(dispatchers.default) {
            either {
                applySorting(
                    userWalletId = userWalletId,
                    tokens = sortTokens(sortedTokensIds, getTokens(userWalletId)),
                    isGrouped = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )
            }
        }
    }

    private suspend fun Raise<TokenListSortingError>.sortTokens(
        sortedTokensIds: Set<Pair<Network.ID, Token.ID>>,
        unsortedTokens: Set<Token>,
    ): Set<Token> = withContext(dispatchers.default) {
        val nonEmptySortedTokensIds = ensureNotNull(sortedTokensIds.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }

        val sortedTokens = sortedMapOf<Int, Token>()

        unsortedTokens.forEach { token ->
            val index = nonEmptySortedTokensIds.indexOfFirst { (networkId, tokenId) ->
                networkId == token.networkId && tokenId == token.id
            }

            if (index >= 0) {
                sortedTokens[index] = token
            } else {
                raise(TokenListSortingError.UnableToSortTokenList)
            }
        }

        ensureNotNull(sortedTokens.values.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.getTokens(userWalletId: UserWalletId): Set<Token> {
        val tokens = catch(
            block = { tokensRepository.getMultiCurrencyWalletTokens(userWalletId, refresh = false).firstOrNull() },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )

        return ensureNotNull(tokens?.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.applySorting(
        userWalletId: UserWalletId,
        tokens: Set<Token>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ) = withContext(dispatchers.io) {
        catch(
            block = { tokensRepository.saveTokens(userWalletId, tokens, isGrouped, isSortedByBalance) },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )
    }
}