package com.tangem.datasource.local.onramp.sepa

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampCountry

data class OnrampSepaAvailabilityStoreKey(
    val userWallet: UserWallet,
    val country: OnrampCountry,
    val cryptoCurrency: CryptoCurrency,
)