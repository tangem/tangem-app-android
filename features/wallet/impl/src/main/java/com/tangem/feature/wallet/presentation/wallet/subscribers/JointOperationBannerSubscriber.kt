package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.usecase.GetPendingJointOperationsUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetJointOperationBannersTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.JointOperationBannerConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Keeps the wallet screen's pending joint operation banners in step with the wallet's joint accounts. A wallet
 * without them never leaves the empty state, so nothing is announced and nothing is subscribed to downstream.
 */
internal class JointOperationBannerSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val stateController: WalletStateController,
    private val getPendingJointOperationsUseCase: GetPendingJointOperationsUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val clickIntents: WalletClickIntents,
) : BasicWalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        // The subtitle carries the transfer amount, so it is rebuilt when balances are hidden or shown again
        return combine(
            flow = operationsFlow(),
            flow2 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
        ) { operations, isBalanceHidden ->
            val converter = JointOperationBannerConverter(
                isBalanceHidden = isBalanceHidden,
                onBannerClick = { operation ->
                    clickIntents.onJointOperationBannerClick(
                        userWalletId = userWallet.walletId,
                        currency = operation.transfer.currency,
                        operationId = operation.operationId,
                    )
                },
            )
            stateController.update(
                SetJointOperationBannersTransformer(
                    userWalletId = userWallet.walletId,
                    banners = converter.convert(operations),
                ),
            )
        }
    }

    private fun operationsFlow(): Flow<List<JointPendingOperation>> {
        return jointAccountsFlow()
            .flatMapLatest { accounts ->
                if (accounts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    getPendingJointOperationsUseCase(userWalletId = userWallet.walletId, accounts = accounts)
                }
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