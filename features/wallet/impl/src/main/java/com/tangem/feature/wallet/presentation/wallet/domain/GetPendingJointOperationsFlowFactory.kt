package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.usecase.GetPendingJointOperationsUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * The wallet's pending joint operations. Their banners are system cards, so the promo cards follow them as well as
 * the banners do. A wallet without joint accounts gets an empty list as soon as its accounts are known.
 */
@ModelScoped
internal class GetPendingJointOperationsFlowFactory @Inject constructor(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getPendingJointOperationsUseCase: GetPendingJointOperationsUseCase,
) {

    fun create(userWallet: UserWallet): Flow<List<JointPendingOperation>> {
        return jointAccountsFlow(userWallet)
            .flatMapLatest { accounts ->
                if (accounts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    getPendingJointOperationsUseCase(userWalletId = userWallet.walletId, accounts = accounts)
                }
            }
    }

    private fun jointAccountsFlow(userWallet: UserWallet): Flow<List<Account.Joint>> {
        return singleAccountStatusListSupplier(params = SingleAccountStatusListProducer.Params(userWallet.walletId))
            .distinctUntilChanged()
            .conflate()
            .map { accountList ->
                accountList.accountStatuses
                    .filterIsInstance<AccountStatus.Joint>()
                    .filter { it.value is JointAccountStatusValue.Active }
                    .map(AccountStatus.Joint::account)
            }
            .distinctUntilChanged()
    }
}