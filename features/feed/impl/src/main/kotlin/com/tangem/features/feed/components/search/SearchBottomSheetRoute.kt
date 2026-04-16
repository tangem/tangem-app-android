package com.tangem.features.feed.components.search

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.search.model.UserAssetSearchEntry

internal sealed interface SearchBottomSheetRoute {

    data class TokenSelector(
        val entries: List<UserAssetSearchEntry>,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val onTokenSelected: (UserAssetSearchEntry) -> Unit,
        val onDismiss: () -> Unit,
    ) : SearchBottomSheetRoute
}