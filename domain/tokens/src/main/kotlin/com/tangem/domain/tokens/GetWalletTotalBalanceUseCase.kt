package com.tangem.domain.tokens

import arrow.atomic.update
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.tokens.operations.BaseCurrenciesStatusesOperations
import com.tangem.domain.tokens.operations.TokenListFiatBalanceOperations
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber

class GetWalletTotalBalanceUseCase(
    private val currenciesStatusesOperations: BaseCurrenciesStatusesOperations,
) {

    operator fun invoke(
        userTallestIds: Collection<UserWalletId>,
    ): LceFlow<TokenListError, Map<UserWalletId, TotalFiatBalance>> {
        val flows = userTallestIds.distinct()
            .map { userWalletId ->
                invoke(userWalletId).map { maybeBalance ->
                    userWalletId to maybeBalance
                }
            }

        return combine(flows) { balances ->
            lce {
                balances.fold(mutableMapOf()) { acc, (userWalletId, maybeBalance) ->
                    val balance = maybeBalance.fold(
                        ifLoading = { TotalFiatBalance.Loading },
                        ifContent = { it },
                        ifError = {
                            Timber.e("failed to load balances with error: $it")
                            TotalFiatBalance.Failed
                        },
                    )

                    isLoading.update { it || balance is TotalFiatBalance.Loading }

                    acc[userWalletId] = balance
                    acc
                }
            }
        }
    }

    operator fun invoke(userWalletId: UserWalletId): LceFlow<TokenListError, TotalFiatBalance> {
        return currenciesStatusesOperations.getCurrenciesStatuses(userWalletId).map(::createBalance)
    }

    private fun createBalance(
        maybeStatuses: Lce<TokenListError, List<CryptoCurrencyStatus>>,
    ): Lce<TokenListError, TotalFiatBalance> = lce {
        val statuses = when (maybeStatuses) {
            is Lce.Content -> maybeStatuses.content
            is Lce.Error -> raise(maybeStatuses)
            is Lce.Loading -> {
                val content = maybeStatuses.partialContent

                if (content == null) {
                    isLoading.set(true)

                    raise(lceLoading())
                } else {
                    content
                }
            }
        }

        val operations = TokenListFiatBalanceOperations(
            currencies = ensureNotNull(statuses.toNonEmptyListOrNull()) { lceLoading() },
            isAnyTokenLoading = false,
        )

        operations.calculateFiatBalance()
    }
}