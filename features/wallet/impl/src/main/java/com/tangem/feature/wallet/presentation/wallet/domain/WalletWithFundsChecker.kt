package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.extensions.isZero
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import javax.inject.Inject

internal class WalletWithFundsChecker @Inject constructor(
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
) {

    suspend fun check(tokenList: TokenList) {
        val hasNonZeroWallets = tokenList.flattenCurrencies().hasNonZeroWallets()

        if (hasNonZeroWallets) setWalletWithFundsFoundUseCase()
    }

    private fun List<CryptoCurrencyStatus>.hasNonZeroWallets(): Boolean {
        return any {
            val amount = it.value.amount ?: return@any false
            !amount.isZero()
        }
    }
}