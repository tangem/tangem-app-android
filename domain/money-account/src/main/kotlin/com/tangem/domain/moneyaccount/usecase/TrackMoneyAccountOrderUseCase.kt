package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.usecase.StartTangemPayOrderPollingUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch

class TrackMoneyAccountOrderUseCase(
    private val orderRepository: MoneyAccountOrderRepository,
    private val startPolling: StartTangemPayOrderPollingUseCase,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    operator fun invoke(userWalletId: UserWalletId, order: TangemPayOrderInfo) {
        orderRepository.storeOrder(userWalletId = userWalletId, order = order)
        appCoroutineScope.launch {
            track(userWalletId = userWalletId, order = order)
        }
    }

    private suspend fun track(userWalletId: UserWalletId, order: TangemPayOrderInfo) {
        startPolling(
            order = order,
            userWalletId = userWalletId,
            onOrderStateChange = { updated ->
                orderRepository.storeOrder(userWalletId = userWalletId, order = updated)
                moneyAccountStatusFetcher.invoke(userWalletId)
            },
            onTerminalRefresh = { walletId -> moneyAccountStatusFetcher(walletId) },
        )
    }
}