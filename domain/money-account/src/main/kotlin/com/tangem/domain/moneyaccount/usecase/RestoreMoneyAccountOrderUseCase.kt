package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.usecase.FindMoneyAccountCreationOrderUseCase

class RestoreMoneyAccountOrderUseCase(
    private val trackOrder: TrackMoneyAccountOrderUseCase,
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        // TODO v_rodionov: [REDACTED_TASK_KEY] handle error of order restoring
        val order = findMoneyAccountCreationOrderUseCase.invoke(userWalletId).getOrNull() ?: return
        trackOrder(
            userWalletId = userWalletId,
            order = TangemPayOrderInfo.fromOrder(order),
        )
    }
}