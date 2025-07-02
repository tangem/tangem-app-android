package com.tangem.domain.tokens.model.staking

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.Yield

fun Yield.getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?) =
    tokens.firstOrNull { rawCurrencyId?.value == it.coinGeckoId } ?: token