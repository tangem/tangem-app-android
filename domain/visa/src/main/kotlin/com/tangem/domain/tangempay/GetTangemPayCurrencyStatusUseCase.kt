package com.tangem.domain.tangempay

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal

interface GetTangemPayCurrencyStatusUseCase {

    suspend operator fun invoke(
        currency: CryptoCurrency,
        cryptoAmount: BigDecimal,
        fiatAmount: BigDecimal,
        depositAddress: String,
    ): CryptoCurrencyStatus?
}