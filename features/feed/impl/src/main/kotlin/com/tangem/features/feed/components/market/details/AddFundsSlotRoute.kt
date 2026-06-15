package com.tangem.features.feed.components.market.details

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.serialization.Serializable

@Serializable
internal data class AddFundsSlotRoute(
    val rawCurrencyId: CryptoCurrency.RawID,
) : Route