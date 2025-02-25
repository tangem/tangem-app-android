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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
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

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId): LceFlow<TokenListError, TotalFiatBalance> {
        return currenciesStatusesOperations.getCurrenciesStatuses(userWalletId)
            .transformLatest { maybeStatuses ->
                val balance = createBalance(maybeStatuses)

                emit(balance)
            }
    }

    private fun createBalance(
        maybeStatuses: Lce<TokenListError, List<CryptoCurrencyStatus>>,
    ): Lce<TokenListError, TotalFiatBalance> = lce {
        val statuses = maybeStatuses.bind()

        val operations = TokenListFiatBalanceOperations(
            currencies = ensureNotNull(statuses.toNonEmptyListOrNull()) { lceLoading() },
            isAnyTokenLoading = false,
        )

        operations.calculateFiatBalance()
    }
}
