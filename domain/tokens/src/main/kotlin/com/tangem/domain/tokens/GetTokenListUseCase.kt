package com.tangem.domain.tokens

import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.mapper.mapToTokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.operations.CurrenciesStatusesLceOperations
import com.tangem.domain.tokens.operations.TokenListOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class GetTokenListUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId): LceFlow<TokenListError, TokenList> {
        val operations = CurrenciesStatusesLceOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
        )

        return operations.getCurrenciesStatuses(userWalletId).transformLatest { maybeCurrencies ->
            maybeCurrencies.fold(
                ifLoading = { maybeContent ->
                    if (maybeContent != null) {
                        emitAll(createTokenList(userWalletId, maybeContent, isCurrenciesLoading = true))
                    } else {
                        return@transformLatest
                    }
                },
                ifContent = { content ->
                    emitAll(createTokenList(userWalletId, content, isCurrenciesLoading = false))
                },
                ifError = { error -> emit(error.lceError()) },
            )
        }
    }

    private fun createTokenList(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrencyStatus>,
        isCurrenciesLoading: Boolean,
    ): LceFlow<TokenListError, TokenList> {
        val operations = TokenListOperations(
            userWalletId = userWalletId,
            tokens = currencies,
            currenciesRepository = currenciesRepository,
        )

        return operations.getTokenListFlow().map { maybeTokenList ->
            maybeTokenList
                .mapLeft(TokenListOperations.Error::mapToTokenListError)
                .toLce(isCurrenciesLoading)
        }
    }
}
