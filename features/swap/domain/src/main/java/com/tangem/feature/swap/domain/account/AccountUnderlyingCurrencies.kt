package com.tangem.feature.swap.domain.account

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import javax.inject.Inject

/**
 * Currencies a special account holds "under the hood" — one per token per network it is issued on, or a
 * single one while the account is not multichain. Single seam to extend for other account types later
 * without touching SwapModel.
 */
interface AccountUnderlyingCurrencies {

    suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus>
}

internal class PaymentAccountUnderlyingCurrencies @Inject constructor(
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
) : AccountUnderlyingCurrencies {

    override suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus> =
        getPaymentAccountCryptoCurrencyStatusUseCase.invokeSyncCurrencies(userWalletId)
}