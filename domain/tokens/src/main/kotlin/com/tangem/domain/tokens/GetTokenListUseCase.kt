package com.tangem.domain.tokens

import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.staking.repositories.StakingRepository
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
    private val stakingRepository: StakingRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launch(userWalletId: UserWalletId): LceFlow<TokenListError, TokenList> {
        val operations = CurrenciesStatusesLceOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )

        return operations.getCurrenciesStatuses(userWalletId).transformLatest { maybeCurrencies ->
            maybeCurrencies.fold(
                ifLoading = { maybeContent ->
                    if (maybeContent != null) {
                        emitAll(createTokenListLce(userWalletId, maybeContent, isCurrenciesLoading = true))
                    } else {
                        emit(lceLoading())
                    }
                },
                ifContent = { content ->
                    emitAll(createTokenListLce(userWalletId, content, isCurrenciesLoading = false))
                },
                ifError = { error -> emit(error.lceError()) },
            )
        }
    }

    private fun createTokenListLce(
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
