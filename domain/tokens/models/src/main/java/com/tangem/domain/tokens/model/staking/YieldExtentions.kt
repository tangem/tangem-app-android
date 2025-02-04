package com.tangem.domain.tokens.model.staking

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency

fun Yield.getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?) =
    tokens.firstOrNull { rawCurrencyId?.value == it.coinGeckoId } ?: token