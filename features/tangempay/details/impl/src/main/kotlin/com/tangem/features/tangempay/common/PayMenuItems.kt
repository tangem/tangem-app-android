package com.tangem.features.tangempay.common

import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.isPlanTransitioningState
import com.tangem.features.tangempay.details.impl.R

internal object PayMenuItems {

    private val changePlanLabel = resourceReference(R.string.tangempay_current_plan_change)

    fun changePlan(tariffPlan: TangemPayTariffPlanState, onClick: () -> Unit): TangemMenu.UiState.Item {
        val isPlanChanging = tariffPlan.isPlanTransitioningState
        return TangemMenu.UiState.Item(
            label = changePlanLabel,
            onClick = onClick,
            description = if (isPlanChanging) {
                resourceReference(R.string.tangempay_changing_plan)
            } else {
                resourceReference(R.string.tangempay_plan_menu_subtitle, wrappedList(tariffPlan.tariff.plan.name))
            },
            iconStart = if (isPlanChanging) Icons.ic_arrow_refresh_20 else Icons.ic_info_20,
        )
    }

    fun isChangePlan(entry: TangemMenu.UiState.Entry): Boolean =
        (entry as? TangemMenu.UiState.Item)?.label == changePlanLabel

    fun cashback(label: TextReference, onClick: () -> Unit): TangemMenu.UiState.Item = TangemMenu.UiState.Item(
        label = label,
        onClick = onClick,
        iconStart = Icons.ic_percent_backward_20,
    )

    fun support(onClick: () -> Unit): TangemMenu.UiState.Item = TangemMenu.UiState.Item(
        label = resourceReference(R.string.tangempay_pay_support),
        onClick = onClick,
        iconStart = Icons.ic_mail_20,
    )
}