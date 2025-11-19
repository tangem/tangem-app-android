package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.extensions.isZero
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ModelScoped
internal class WalletWithFundsChecker @Inject constructor(
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
) {

    private val statusByWalletId = ConcurrentHashMap<UserWalletId, Boolean>()

    suspend fun check(tokenList: TokenList) {
        val hasNonZeroWallets = tokenList.flattenCurrencies().hasNonZeroWallets()

        if (hasNonZeroWallets) setWalletWithFundsFoundUseCase()
    }

    suspend fun check(userWalletId: UserWalletId, currencies: List<CryptoCurrencyStatus>) {
        val hasNonZeroWallets = currencies.hasNonZeroWallets()
        val prevStatus = statusByWalletId.get(userWalletId)

        if (hasNonZeroWallets && prevStatus != true) {
            statusByWalletId[userWalletId] = true
            setWalletWithFundsFoundUseCase()
        }
    }

    private fun List<CryptoCurrencyStatus>.hasNonZeroWallets(): Boolean {
        return any {
            val amount = it.value.amount ?: return@any false
            !amount.isZero()
        }
    }
}