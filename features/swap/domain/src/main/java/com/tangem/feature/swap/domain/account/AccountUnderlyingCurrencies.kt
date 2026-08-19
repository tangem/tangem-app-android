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

    /**
     * The subset of [get] that can actually receive a withdrawal. The withdraw endpoint takes an amount and a
     * recipient address but no network, so the backend can only ever move the account's default currency.
     */
    suspend fun getWithdrawable(userWalletId: UserWalletId): List<CryptoCurrencyStatus>
}

internal class PaymentAccountUnderlyingCurrencies @Inject constructor(
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
) : AccountUnderlyingCurrencies {

    override suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus> =
        getPaymentAccountCryptoCurrencyStatusUseCase.invokeSyncCurrencies(userWalletId)

    override suspend fun getWithdrawable(userWalletId: UserWalletId): List<CryptoCurrencyStatus> {
        val defaultCurrencyStatus = getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId)
            .getOrNull()
            ?.second
        return listOfNotNull(defaultCurrencyStatus)
    }
}