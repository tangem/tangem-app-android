package com.tangem.features.tangempay.tiers.current

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayCurrentPlanModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<TangemPayCurrentPlanComponent.Params>()

    val state: StateFlow<TangemPayCurrentPlanUM>
        field = MutableStateFlow(createState(params.tariffPlan.plan))

    private fun createState(plan: TangemPayTariffPlan): TangemPayCurrentPlanUM = TangemPayCurrentPlanUM(
        planName = stringReference(plan.name),
        notification = null,
        sections = buildSections(plan),
        onBackClick = router::pop,
        onChangePlanClick = { router.push(TangemPayAccountDetailsInnerRoute.SelectPlan) },
    )

    private fun buildSections(plan: TangemPayTariffPlan) = persistentListOf(
        sectionOf(plan, TangemPayTariffPlan.Section.CARD_RELATED, R.string.tangempay_current_plan_section_card),
        sectionOf(plan, TangemPayTariffPlan.Section.PLAN_RELATED, R.string.tangempay_current_plan_section_plan),
    )
        .filter { it.items.isNotEmpty() }
        .toImmutableList()

    private fun sectionOf(
        plan: TangemPayTariffPlan,
        section: TangemPayTariffPlan.Section,
        headerStrRes: Int,
    ): TangemPayCurrentPlanUM.Section {
        return TangemPayCurrentPlanUM.Section(
            header = resourceReference(headerStrRes),
            items = plan.descriptionItems
                .filter { it.section == section }
                .sortedBy { it.order }
                .map { item ->
                    TangemPayCurrentPlanUM.InfoItem(
                        label = stringReference(item.title),
                        value = stringReference(item.body),
                    )
                }
                .toImmutableList(),
        )
    }
}