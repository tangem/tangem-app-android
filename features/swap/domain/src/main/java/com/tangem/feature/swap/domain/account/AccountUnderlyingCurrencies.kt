package com.tangem.feature.swap.domain.account

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import javax.inject.Inject

/**
 * Currencies a special account holds "under the hood". Today the Payment account returns exactly one
 * (USDC Polygon). Single seam to extend for multichain/Polymarket later without touching SwapModel.
 */
interface AccountUnderlyingCurrencies {
    suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus>
}

internal class PaymentAccountUnderlyingCurrencies @Inject constructor(
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
) : AccountUnderlyingCurrencies {

    override suspend fun get(userWalletId: UserWalletId): List<CryptoCurrencyStatus> {
        val cryptoCurrencyStatus = getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId)
            .getOrNull()
            ?.second
        return listOfNotNull(cryptoCurrencyStatus)
    }
}