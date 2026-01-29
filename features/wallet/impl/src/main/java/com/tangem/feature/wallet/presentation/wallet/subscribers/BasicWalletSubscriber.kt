package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
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

    /** Supplier to get the account status for the wallet */
    abstract val singleAccountStatusListSupplier: SingleAccountStatusListSupplier

    /**
     * Provides a flow of [AccountStatusList] for the associated user wallet.
     *
     * @return A [Flow] emitting the [AccountStatusList] of the wallet, distinct and conflated.
     */
    protected fun getAccountStatusListFlow(): Flow<AccountStatusList> {
        return singleAccountStatusListSupplier(
            params = SingleAccountStatusListProducer.Params(userWallet.walletId),
        )
            .distinctUntilChanged()
            .conflate()
    }

    /**
     * Provides a flow of [AccountStatus] for the associated user wallet.
     *
     * @return A [Flow] emitting the [AccountStatus] of the wallet, distinct and conflated.
     */
    protected fun getMainAccountStatusFlow(): Flow<AccountStatus.Crypto.Portfolio> {
        return getAccountStatusListFlow()
            .mapNotNull { accountStatusList ->
                accountStatusList.accountStatuses.filterIsInstance<AccountStatus.Crypto>().find { accountStatus ->
                    when (accountStatus) {
                        is AccountStatus.Crypto.Portfolio -> accountStatus.account.isMainAccount
                    }
                } as? AccountStatus.Crypto.Portfolio
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