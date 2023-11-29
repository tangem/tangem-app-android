package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import com.tangem.common.extensions.isZero
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import javax.inject.Inject

internal class WalletWithFundsChecker @Inject constructor(
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
) {

    suspend fun check(maybeTokenList: Either<TokenListError, TokenList>) {
        val tokenList = (maybeTokenList as? Either.Right)?.value ?: return

        val hasNonZeroWallets = when (tokenList) {
            is TokenList.GroupedByNetwork -> {
                tokenList.groups
                    .flatMap(NetworkGroup::currencies)
                    .hasNonZeroWallets()
            }
            is TokenList.Ungrouped -> tokenList.currencies.hasNonZeroWallets()
            is TokenList.Empty -> false
        }

        if (hasNonZeroWallets) setWalletWithFundsFoundUseCase()
    }

    private fun List<CryptoCurrencyStatus>.hasNonZeroWallets(): Boolean {
        return any {
            val amount = it.value.amount ?: return@any false
            !amount.isZero()
        }
    }
}
