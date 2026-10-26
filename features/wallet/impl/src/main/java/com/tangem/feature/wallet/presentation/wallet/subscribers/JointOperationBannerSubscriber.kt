package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.GetPendingJointOperationsFlowFactory
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
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val getPendingJointOperationsFlowFactory: GetPendingJointOperationsFlowFactory,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        // The subtitle carries the transfer amount, so it is rebuilt when balances are hidden or shown again
        return combine(
            flow = getPendingJointOperationsFlowFactory.create(userWallet),
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

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): JointOperationBannerSubscriber
    }
}