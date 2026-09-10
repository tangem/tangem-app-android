package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.jointaccount.usecase.GetPendingJointOperationsUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetJointOperationBannersTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.JointOperationBannerConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Keeps the wallet screen's pending joint operation banner in step with the wallet's joint accounts. A wallet
 * without them never leaves the empty state, so nothing is announced and nothing is subscribed to downstream.
 */
internal class JointOperationBannerSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val stateController: WalletStateController,
    private val getPendingJointOperationsUseCase: GetPendingJointOperationsUseCase,
) : BasicWalletSubscriber() {

    private val bannerConverter = JointOperationBannerConverter()

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return jointAccountsFlow()
            .flatMapLatest { accounts ->
                if (accounts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    getPendingJointOperationsUseCase(userWalletId = userWallet.walletId, accounts = accounts)
                }
            }
            .onEach { operations ->
                stateController.update(
                    SetJointOperationBannersTransformer(
                        userWalletId = userWallet.walletId,
                        banners = bannerConverter.convert(operations),
                    ),
                )
            }
    }

    private fun jointAccountsFlow(): Flow<List<Account.Joint>> {
        return getAccountStatusListFlow()
            .map { accountList ->
                accountList.accountStatuses
                    .filterIsInstance<AccountStatus.Joint>()
                    .map(AccountStatus.Joint::account)
            }
            .distinctUntilChanged()
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): JointOperationBannerSubscriber
    }
}