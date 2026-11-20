package com.tangem.domain.tangempay.usecase.order

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.ActiveOrdersRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ApplyActiveOrdersUseCase(
    private val activeOrdersRegistry: ActiveOrdersRegistry,
) {
    operator fun invoke(
        userWalletId: UserWalletId,
        statuses: Flow<AccountStatus.Payment>,
    ): Flow<AccountStatus.Payment> {
        return combine(statuses, activeOrdersRegistry.observe(userWalletId)) { status, activeOrders ->
            status.copy(value = status.value.withActiveOrders(activeOrders))
        }
    }

    private fun PaymentAccountStatusValue.withActiveOrders(
        activeOrders: List<TangemPayOrder>,
    ): PaymentAccountStatusValue {
        if (this !is PaymentAccountStatusValue.Loaded || activeOrders.isEmpty()) return this
        return copy(
            cards = cards.applyOrders(activeOrders),
        )
    }

    private fun List<TangemPayCard>.applyOrders(activeOrders: List<TangemPayOrder>): List<TangemPayCard> {
        val frozenStateOrdersProductInstanceIds = activeOrders.mapNotNullTo(mutableSetOf()) { order ->
            when (order.details) {
                is OrderDetails.CardFreeze -> order.details.productInstanceId
                is OrderDetails.CardUnfreeze -> order.details.productInstanceId
                else -> null
            }
        }
        return map { card ->
            if (card.productInstanceId in frozenStateOrdersProductInstanceIds) {
                card.copy(frozenState = TangemPayCardFrozenState.Pending)
            } else {
                card
            }
        }
    }
}