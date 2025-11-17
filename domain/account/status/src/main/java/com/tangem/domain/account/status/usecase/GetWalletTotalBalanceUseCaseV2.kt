package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListError
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Use case to get total fiat balances for multiple user wallets.
 * It's second version of the use case that will replace the previous one after accounts feature released.
 *
 * @param multiAccountStatusListSupplier Supplier that provides the status list for multiple accounts.
 */
class GetWalletTotalBalanceUseCaseV2(
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
) {

    operator fun invoke(
        userWalletIds: Collection<UserWalletId>,
    ): LceFlow<TokenListError, Map<UserWalletId, TotalFiatBalance>> {
        return multiAccountStatusListSupplier()
            .map { accountStatusLists ->
                accountStatusLists
                    .filter { it.userWalletId in userWalletIds }
                    .associate {
                        it.userWalletId to it.totalFiatBalance
                    }
                    .lceContent()
            }
            .distinctUntilChanged()
    }
}