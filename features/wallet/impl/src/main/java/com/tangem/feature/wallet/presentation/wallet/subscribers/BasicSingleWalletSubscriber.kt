package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * Basic implementation of [WalletSubscriber] for single wallet.
 *
[REDACTED_AUTHOR]
 */
internal abstract class BasicSingleWalletSubscriber : BasicWalletSubscriber() {

    /** Account ID for the main crypto portfolio of the user wallet */
    val accountId: AccountId
        get() = AccountId.forMainCryptoPortfolio(userWalletId = userWallet.walletId)

    /**
     * Provides a flow of the primary [CryptoCurrencyStatus] for the associated user wallet.
     *
     * @return A [Flow] emitting the primary [CryptoCurrencyStatus] of the wallet, distinct and conflated.
     */
    protected fun getPrimaryCurrencyStatusFlow(): Flow<CryptoCurrencyStatus> {
        return getMainAccountStatusFlow()
            .mapNotNull { accountStatus ->
                accountStatus.flattenCurrencies().firstOrNull()
            }
            .distinctUntilChanged()
            .conflate()
    }
}