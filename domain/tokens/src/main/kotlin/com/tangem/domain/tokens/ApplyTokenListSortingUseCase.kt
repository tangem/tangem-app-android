package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class ApplyTokenListSortingUseCase(
    private val tokensRepository: TokensRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sortedTokens: Set<Token>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokenListSortingError, Unit> {
        return withContext(dispatchers.default) {
            either {
                val nonEmptyTokens = ensureNotNull(sortedTokens.toNonEmptySetOrNull()) {
                    TokenListSortingError.TokenListIsEmpty
                }

                applySorting(userWalletId, nonEmptyTokens, isGrouped, isSortedByBalance)
            }
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