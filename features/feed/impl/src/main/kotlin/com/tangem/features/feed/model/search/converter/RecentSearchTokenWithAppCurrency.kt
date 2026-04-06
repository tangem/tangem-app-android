package com.tangem.features.feed.model.search.converter

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.search.model.RecentSearchToken

internal data class RecentSearchTokenWithAppCurrency(
    val token: RecentSearchToken,
    val appCurrency: AppCurrency,
)