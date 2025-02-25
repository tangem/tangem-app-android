package com.tangem.domain.tokens.operations

import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Base operations for working with currencies statuses
 *
[REDACTED_AUTHOR]
 */
interface BaseCurrenciesStatusesOperations {

    /** Get [LceFlow] of currencies statuses by [userWalletId] */
    fun getCurrenciesStatuses(userWalletId: UserWalletId): LceFlow<TokenListError, List<CryptoCurrencyStatus>>
}