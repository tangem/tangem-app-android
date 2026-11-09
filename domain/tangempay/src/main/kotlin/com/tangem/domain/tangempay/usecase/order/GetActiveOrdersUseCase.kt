package com.tangem.domain.tangempay.usecase.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.ActiveOrdersRegistry

class GetActiveOrdersUseCase(
    private val activeOrdersRegistry: ActiveOrdersRegistry,
) {
    operator fun invoke(userWalletId: UserWalletId): List<TangemPayOrder> {
        return activeOrdersRegistry.get(userWalletId)
    }
}

fun GetActiveOrdersUseCase.hasCardFrozenStateOrderFor(
    productInstanceId: String,
    userWalletId: UserWalletId,
): Boolean {
    return invoke(userWalletId).any { order ->
        when (val details = order.details) {
            is OrderDetails.CardFreeze -> details.productInstanceId == productInstanceId
            is OrderDetails.CardUnfreeze -> details.productInstanceId == productInstanceId
            else -> false
        }
    }
}