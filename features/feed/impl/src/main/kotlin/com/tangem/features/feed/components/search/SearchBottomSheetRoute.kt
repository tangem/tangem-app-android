package com.tangem.features.feed.components.search

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry

internal sealed interface SearchBottomSheetRoute {

    data class TokenSelector(
        val entries: List<UserAssetEntry>,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val onTokenSelected: (UserAssetEntry) -> Unit,
        val onDismiss: () -> Unit,
    ) : SearchBottomSheetRoute
}