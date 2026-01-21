package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * Basic implementation of [WalletSubscriber] for wallet.
 *
[REDACTED_AUTHOR]
 */
internal abstract class BasicWalletSubscriber : WalletSubscriber() {

    /** User wallet associated with this subscriber */
    abstract val userWallet: UserWallet

    abstract val accountsSharedFlowHolder: AccountsSharedFlowHolder

    /**
     * Provides a flow of [AccountStatusList] for the associated user wallet.
     *
     * @return A [Flow] emitting the [AccountStatusList] of the wallet, distinct and conflated.
     */
    protected fun getAccountStatusListFlow(): Flow<AccountStatusList> {
        return accountsSharedFlowHolder.getAccountStatusListFlow(userWallet.walletId)
    }

    /**
     * Provides a flow of [AccountStatus] for the associated user wallet.
     *
     * @return A [Flow] emitting the [AccountStatus] of the wallet, distinct and conflated.
     */
    protected fun getMainAccountStatusFlow(): Flow<AccountStatus.CryptoPortfolio> {
        return getAccountStatusListFlow()
            .mapNotNull { accountStatusList ->
                accountStatusList.accountStatuses.find { accountStatus ->
                    when (accountStatus) {
                        is AccountStatus.CryptoPortfolio -> accountStatus.account.isMainAccount
                    }
                } as? AccountStatus.CryptoPortfolio
            }
            .distinctUntilChanged()
            .conflate()
    }

    protected fun getCryptoCurrencyStatusesFlow(): Flow<List<CryptoCurrencyStatus>> {
        return getAccountStatusListFlow()
            .mapNotNull(AccountStatusList::flattenCurrencies)
            .distinctUntilChanged()
            .conflate()
    }
}