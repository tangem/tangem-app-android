package com.tangem.features.onramp.utils.model

import com.tangem.domain.onramp.model.OnrampCurrency

internal val EUR_CURRENCY = OnrampCurrency(
    code = "EUR",
    name = "Euro",
    unit = "€",
    precision = 2,
    image = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/Currencies/EUR.png",
)