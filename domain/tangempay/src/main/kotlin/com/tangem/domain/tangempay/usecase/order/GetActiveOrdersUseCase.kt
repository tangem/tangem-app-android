package com.tangem.domain.tangempay.usecase.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.ActiveOrdersRegistry

class GetActiveOrdersUseCase(
    private val activeOrdersRegistry: ActiveOrdersRegistry,
) {
    operator fun invoke(userWalletId: UserWalletId): List<TangemPayOrder> {
        return activeOrdersRegistry.get(userWalletId)
    }
}