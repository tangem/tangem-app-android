package com.tangem.data.pay.util

import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance

internal object PlasticCardStateResolver {

    private val TERMINAL_STATUSES = setOf(
        ProductInstance.Status.BLOCKED,
        ProductInstance.Status.DEACTIVATING,
        ProductInstance.Status.DEACTIVATED,
        ProductInstance.Status.CANCELED,
    )

    fun isAwaitingActivation(cardInfo: CardInfo, productInstance: ProductInstance): Boolean {
        return cardInfo.cardType == TangemPayCardType.PHYSICAL &&
            cardInfo.cardStatus == TangemPayCard.Status.INACTIVE &&
            productInstance.status !in TERMINAL_STATUSES
    }
}