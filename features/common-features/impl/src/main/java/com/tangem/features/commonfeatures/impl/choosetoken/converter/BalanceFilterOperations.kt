package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.EmptyReason
import java.math.BigDecimal

/** A balance counts as zero only when it is known and <= 0. Unknown (null) balances are never treated as zero. */
internal fun isZeroBalance(amount: BigDecimal?): Boolean {
    if (amount == null) return false
    return amount.signum() <= 0
}

internal fun CryptoCurrencyStatus.isZeroBalance(): Boolean = isZeroBalance(value.amount)

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