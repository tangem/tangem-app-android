package com.tangem.features.tangempay.tiers.current

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull
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
        field = MutableStateFlow(createState(params.tariffPlan))

    private fun createState(customerPlan: TangemPayCustomerTariffPlan): TangemPayCurrentPlanUM = TangemPayCurrentPlanUM(
        planName = stringReference(customerPlan.plan.name),
        notification = createNotification(customerPlan),
        sections = buildSections(customerPlan.plan),
        onBackClick = router::pop,
        onChangePlanClick = { router.push(TangemPayAccountDetailsInnerRoute.SelectPlan) },
    )

    // TODO v_rodionov: strings hardcoded for now - wait for localization
    private fun createNotification(customerPlan: TangemPayCustomerTariffPlan): TangemPayCurrentPlanUM.Notification? {
        val date = customerPlan.formatNextBillingDateOrNull(formatter = DateTimeFormatters.dateMMMd) ?: return null
        val feeText = customerPlan.plan.formatRecurringFeeOrNull() ?: return null
        return when (customerPlan.status) {
            TangemPayCustomerTariffPlan.Status.DOWNGRADE_PENDING -> {
                val targetPlan = customerPlan.pendingPlan ?: return null
                TangemPayCurrentPlanUM.Notification(
                    text = stringReference(
                        "Your ${customerPlan.plan.name} plan is active till $date, then we will move you to " +
                            "${targetPlan.name}. $feeText won't be charged.",
                    ),
                    button = TangemPayCurrentPlanUM.Notification.Button(
                        text = stringReference("Stay on ${customerPlan.plan.name}"),
                        onClick = {}, // TODO v_rodionov: #[REDACTED_TASK_KEY] - Downgrade
                    ),
                )
            }
            TangemPayCustomerTariffPlan.Status.ACTIVE -> TangemPayCurrentPlanUM.Notification(
                text = stringReference("$feeText monthly fee will be charged on $date"),
            )
            else -> null
        }
    }

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