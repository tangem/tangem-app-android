package com.tangem.domain.pay.model

import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials

sealed interface CustomerOrderRequest {

    data class CardIssue(
        val type: OrderType,
        val specificationName: String,
        val deeplinkValue: String? = null,
    ) : CustomerOrderRequest

    data class PlasticCardIssue(
        val specificationName: String,
        val order: PlasticCardOrder,
    ) : CustomerOrderRequest

    data class PlasticCardReissue(
        val sourceProductInstanceId: String,
        val order: PlasticCardOrder,
    ) : CustomerOrderRequest

    data class CardActivation(val order: CardActivationOrder) : CustomerOrderRequest

    data class TariffPlanTransition(
        val targetTariffPlanId: String,
        val transitionType: TangemPayTariffPlanTransition.Type,
    ) : CustomerOrderRequest

    data class SmartContractIssue(val chainId: Int) : CustomerOrderRequest

    data class Onboarding(
        val tariffPlanTransition: TariffPlanTransition?,
        val depositCredentials: MoneyAccountDepositCredentials?,
    ) : CustomerOrderRequest
}