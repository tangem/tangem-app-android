package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampAmount

data class OnrampLastUpdate(
    val lastAmount: OnrampAmount,
    val lastCountryString: String,
)