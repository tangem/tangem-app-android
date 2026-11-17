package com.tangem.features.earn.lending.impl.session

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class LendingAccountNames @Inject constructor(
    private val accountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    /**
     * Falls back to the main account name: every wallet has one, and the result must not wait on a name. The supplier
     * is a hot flow that stays silent while the accounts cannot be read, so the lookup is bounded.
     */
    suspend fun nameOf(userWalletId: UserWalletId, currencyId: CryptoCurrency.ID): TextReference {
        val name = withTimeoutOrNull(LOOKUP_TIMEOUT) { accountStatusListSupplier(userWalletId).firstOrNull() }
            ?.accountStatuses
            ?.filterIsInstance<AccountStatus.CryptoPortfolio>()
            ?.firstOrNull { status -> status.flattenCurrencies().any { it.currency.id == currencyId } }
            ?.account
            ?.accountName

        return (name ?: AccountName.DefaultMain).toUM().value
    }

    private companion object {
        val LOOKUP_TIMEOUT = 2.seconds
    }
}