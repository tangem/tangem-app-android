package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ApplyTokenListSortingUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sortedTokensIds: List<Pair<Network.ID, CryptoCurrency.ID>>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokenListSortingError, Unit> {
        return withContext(dispatchers.default) {
            either {
                applySorting(
                    userWalletId = userWalletId,
                    tokens = sortTokens(sortedTokensIds, getCurrencies(userWalletId)),
                    isGrouped = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )
            }
        }
    }

    private suspend fun Raise<TokenListSortingError>.sortTokens(
        sortedTokensIds: List<Pair<Network.ID, CryptoCurrency.ID>>,
        unsortedTokens: List<CryptoCurrency>,
    ): List<CryptoCurrency> = withContext(dispatchers.default) {
        val nonEmptySortedTokensIds = ensureNotNull(sortedTokensIds.toNonEmptySetOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }

        val sortedTokens = sortedMapOf<Int, CryptoCurrency>()

        unsortedTokens.distinct().forEach { token ->
            val index = nonEmptySortedTokensIds.indexOfFirst { (networkId, tokenId) ->
                networkId == token.networkId && tokenId == token.id
            }

            if (index >= 0) {
                sortedTokens[index] = token
            } else {
                raise(TokenListSortingError.UnableToSortTokenList)
            }
        }

        ensureNotNull(sortedTokens.values.toNonEmptyListOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.getCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        val tokens = catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrencies(userWalletId, refresh = false).firstOrNull()
            },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )

        return ensureNotNull(tokens?.toNonEmptyListOrNull()) {
            TokenListSortingError.TokenListIsEmpty
        }
    }

    private suspend fun Raise<TokenListSortingError>.applySorting(
        userWalletId: UserWalletId,
        tokens: List<CryptoCurrency>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ) = withContext(dispatchers.io) {
        catch(
            block = { currenciesRepository.saveTokens(userWalletId, tokens, isGrouped, isSortedByBalance) },
            catch = { raise(TokenListSortingError.DataError(it)) },
        )
    }
}
