package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.extensions.isZero
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
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