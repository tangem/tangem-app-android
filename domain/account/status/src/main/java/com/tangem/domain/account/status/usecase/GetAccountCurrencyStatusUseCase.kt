package com.tangem.domain.account.status.usecase

import arrow.core.Option
import arrow.core.none
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Use case to retrieve the status of a specific cryptocurrency associated with an account.
 *
[REDACTED_AUTHOR]
 */
// TODO: Implement [REDACTED_JIRA]
class GetAccountCurrencyStatusUseCase {

    /**
     * Invokes the use case to get the [AccountCryptoCurrencyStatus] for the given [currencyId].
     *
     * @param currencyId The ID of the cryptocurrency to look up.
     *
     * @return An [Option] containing the [AccountCryptoCurrencyStatus] if found,
     * or [arrow.core.None] if not found or if any validation fails.
     */
    suspend operator fun invoke(currencyId: CryptoCurrency.ID): Option<AccountCryptoCurrencyStatus> = none()
}