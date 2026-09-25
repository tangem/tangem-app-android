package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.moneyaccount.usecase.RestoreMoneyAccountOrderUseCase
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.RestoreActiveCardOrdersUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
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
    private val restoreMoneyAccountOrderUseCase: RestoreMoneyAccountOrderUseCase,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val analytics: WalletTangemPayAnalyticsEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        coroutineScope.launch {
            // TODO: Doston move this logic to proper place(e.g. WalletBalanceFetcher)
            tangemPayWithdrawRepository.pollWithdrawOrdersIfNeeds(userWallet)
        }
        coroutineScope.launch {
            restoreActiveCardOrdersUseCase(userWallet.walletId)
        }
        if (moneyAccountFeatureToggles.isMoneyAccountsEnabled) {
            coroutineScope.launch {
                restoreMoneyAccountOrderUseCase(userWallet.walletId)
            }
        }
        subscribeToStatus(coroutineScope)
        return emptyFlow<Any>()
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