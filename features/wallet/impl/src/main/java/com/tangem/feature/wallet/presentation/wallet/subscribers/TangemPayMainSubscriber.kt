package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.moneyaccount.usecase.PollTangemPayAccountStatusesUseCase
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.repository.TangemPayWithdrawRepository
import com.tangem.domain.tangempay.usecase.RestoreActiveCardOrdersUseCase
import com.tangem.domain.tangempay.usecase.order.RestoreOrdersUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class TangemPayMainSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val restoreActiveCardOrdersUseCase: RestoreActiveCardOrdersUseCase,
    private val restoreOrdersUseCase: RestoreOrdersUseCase,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val analytics: WalletTangemPayAnalyticsEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> = channelFlow<Any> {
        launch {
            // TODO: Doston move this logic to proper place(e.g. WalletBalanceFetcher)
            tangemPayWithdrawRepository.pollWithdrawOrdersIfNeeds(userWallet)
        }
        launch {
            restoreActiveCardOrdersUseCase(userWallet.walletId)
        }
        if (tangemPayFeatureToggles.isOrderFrameworkEnabled) {
            launch {
                restoreOrdersUseCase(userWallet.walletId)
            }
        }
        if (moneyAccountFeatureToggles.isMoneyAccountsEnabled) {
            pollTangemPayAccountStatusesUseCase(userWallet.walletId).launchIn(this)
        }
        subscribeToStatus(this)
    }

    private fun subscribeToStatus(coroutineScope: CoroutineScope) {
        paymentAccountStatusSupplier.invoke(userWalletId = userWallet.walletId)
            .map { it.value }
            .distinctUntilChanged()
            .onEach(analytics::send)
            .launchIn(coroutineScope)
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): TangemPayMainSubscriber
    }
}