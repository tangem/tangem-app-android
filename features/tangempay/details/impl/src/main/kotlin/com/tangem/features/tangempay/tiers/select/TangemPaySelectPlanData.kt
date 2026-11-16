package com.tangem.features.tangempay.tiers.select

import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition

internal data class TangemPaySelectPlanData(
    val tariffPlan: TangemPayCustomerTariffPlan? = null,
    val transitions: List<TangemPayTariffPlanTransition> = emptyList(),
    val selectedIndex: Int = 0,
    val isConfirm: Boolean = false,
    val isProcessing: Boolean = false,
    val isCompareShown: Boolean = false,
) {
    val allowedTransitions: List<TangemPayTariffPlanTransition> = transitions.filter { it.type in ALLOWED_TYPES }

    val allowedTransitionsForCompare: List<TangemPayTariffPlanTransition> =
        transitions.filter { it.type in ALLOWED_TYPES_FOR_COMPARE }

    val selectedTransition: TangemPayTariffPlanTransition?
        get() = allowedTransitions.getOrNull(selectedIndex)

    companion object {
        private val ALLOWED_TYPES = setOf(
            TangemPayTariffPlanTransition.Type.UPGRADE,
            TangemPayTariffPlanTransition.Type.DOWNGRADE,
            TangemPayTariffPlanTransition.Type.ACTIVATION,
        )
        private val ALLOWED_TYPES_FOR_COMPARE = setOf(
            TangemPayTariffPlanTransition.Type.UPGRADE,
            TangemPayTariffPlanTransition.Type.DOWNGRADE,
        )
    }
}