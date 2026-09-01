package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.EmptyReason

/**
 * FilteredOut only when the HideZero filter removed everything while tokens were actually available and the
 * user is not searching (resetting the filter wouldn't help a search). Otherwise NoTokens.
 */
internal fun resolveEmptyReason(
    balanceFilter: BalanceFilter,
    isSearching: Boolean,
    hasAvailableTokens: Boolean,
): EmptyReason = if (balanceFilter == BalanceFilter.HideZero && !isSearching && hasAvailableTokens) {
    EmptyReason.FilteredOut
} else {
    EmptyReason.NoTokens
}