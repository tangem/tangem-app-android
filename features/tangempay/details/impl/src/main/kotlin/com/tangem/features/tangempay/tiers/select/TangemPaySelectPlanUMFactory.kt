package com.tangem.features.tangempay.tiers.select

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.account.mainImageUrl
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TangemPaySelectPlanUMFactory(
    private val mode: TangemPaySelectPlanComponent.Mode,
    private val clicks: TangemPaySelectPlanClickIntents,
) {
    fun createLoading(data: TangemPaySelectPlanData): TangemPaySelectPlanUM {
        return build(
            data = data,
            content = TangemPaySelectPlanUM.Content.Loading,
        )
    }

    fun createError(data: TangemPaySelectPlanData): TangemPaySelectPlanUM {
        return build(
            data = data,
            content = TangemPaySelectPlanUM.Content.Error(onRetryClick = clicks::onRetryClick),
        )
    }

    fun create(data: TangemPaySelectPlanData): TangemPaySelectPlanUM {
        return build(
            data = data,
            content = if (data.isConfirm) buildConfirmContent(data) else buildSelectContent(data),
        )
    }

    private fun build(data: TangemPaySelectPlanData, content: TangemPaySelectPlanUM.Content): TangemPaySelectPlanUM {
        return TangemPaySelectPlanUM(
            topBarTitle = resourceReference(
                if (data.isConfirm) {
                    R.string.tangempay_select_plan_confirm_title
                } else {
                    R.string.tangempay_select_plan_title
                },
            ),
            plans = data.allowedTransitions.map { it.plan.toPlanUM() }.toImmutableList(),
            selectedIndex = data.selectedIndex,
            onPlanSelected = clicks::onPlanSelected,
            onBackClick = clicks::onBackClick,
            onCloseClick = clicks::onCloseClick,
            onLinkClick = clicks::onLinkClick,
            content = content,
            compare = if (data.isCompareShown) buildCompare(data) else null,
        )
    }

    private fun buildSelectContent(data: TangemPaySelectPlanData) = TangemPaySelectPlanUM.Content.Select(
        isProcessing = data.isProcessing,
        selectButtonIconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24)
            .takeIf { isMoneyAccountActivationRequired() },
        onComparePlansClick = clicks::onComparePlansClick,
        onSelectClick = clicks::onSelectClick,
    )

    private fun isMoneyAccountActivationRequired(): Boolean {
        return mode is TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder &&
            TangemPayProduct.BANKING_ACCOUNT in mode.availableProducts
    }

    private fun buildCompare(data: TangemPaySelectPlanData): TangemPaySelectPlanUM.ComparePlans {
        val plans = listOfNotNull(data.tariffPlan?.plan) + data.allowedTransitionsForCompare.map { it.plan }
        val orderedTitles = plans
            .flatMap { plan -> plan.descriptionItems.filter { it.section in COMPARE_SECTIONS } }
            .sortedWith(compareBy({ it.section.ordinal }, { it.order }))
            .map { it.title }
            .distinct()
        return TangemPaySelectPlanUM.ComparePlans(
            attributes = orderedTitles.map(::stringReference).toImmutableList(),
            plans = plans.map { plan ->
                val valueByTitle = plan.descriptionItems
                    .filter { it.section in COMPARE_SECTIONS }
                    .associate { it.title to it.body }
                TangemPaySelectPlanUM.ComparePlans.Plan(
                    name = stringReference(plan.name),
                    values = orderedTitles
                        .map { title -> stringReference(valueByTitle[title].orEmpty()) }
                        .toImmutableList(),
                )
            }.toImmutableList(),
            onDismiss = clicks::onCompareDismiss,
        )
    }

    private fun buildConfirmContent(data: TangemPaySelectPlanData): TangemPaySelectPlanUM.Content {
        val transition = data.selectedTransition ?: return buildSelectContent(data)
        val targetPlan = transition.plan
        val currentTariff = data.tariffPlan
        return TangemPaySelectPlanUM.Content.Confirm(
            title = when (transition.type) {
                TangemPayTariffPlanTransition.Type.UPGRADE -> resourceReference(
                    R.string.tangempay_select_plan_confirm_upgrade_title,
                    wrappedList(targetPlan.programName),
                )
                TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                    val nextBillingDate = currentTariff.nextBillingDate()
                    val currentPlan = currentTariff?.plan
                    if (nextBillingDate != null && currentPlan != null) {
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_downgrade_title,
                            wrappedList(currentPlan.name, currentPlan.programName, nextBillingDate),
                        )
                    } else {
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_switch_title,
                            wrappedList(targetPlan.name),
                        )
                    }
                }
                else -> resourceReference(
                    R.string.tangempay_select_plan_confirm_switch_title,
                    wrappedList(targetPlan.name),
                )
            },
            points = buildConfirmPoints(transition = transition, currentTariff = currentTariff),
            confirmButtonText = resourceReference(
                when (transition.type) {
                    TangemPayTariffPlanTransition.Type.UPGRADE -> R.string.tangempay_select_plan_btn_upgrade
                    TangemPayTariffPlanTransition.Type.DOWNGRADE -> R.string.tangempay_select_plan_btn_downgrade
                    else -> R.string.common_continue
                },
            ),
            isProcessing = data.isProcessing,
            onCancelClick = clicks::onBackClick,
            onConfirmClick = clicks::onConfirmClick,
        )
    }

    private fun buildConfirmPoints(
        transition: TangemPayTariffPlanTransition,
        currentTariff: TangemPayCustomerTariffPlan?,
    ): ImmutableList<TangemPaySelectPlanUM.PointUM> {
        val programName = transition.plan.programName
        return when (transition.type) {
            TangemPayTariffPlanTransition.Type.UPGRADE -> {
                val feeText = transition.plan.formatRecurringFeeOrNull()
                buildList {
                    add(
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_virtual_card,
                            wrappedList(programName),
                        ),
                    )
                    if (feeText != null) {
                        add(
                            resourceReference(
                                R.string.tangempay_select_plan_confirm_point_monthly_fee,
                                wrappedList(feeText),
                            ),
                        )
                    }
                }
            }
            TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                val date = currentTariff.nextBillingDate()
                val currentPlan = currentTariff?.plan
                buildList {
                    if (date != null) {
                        add(
                            resourceReference(
                                R.string.tangempay_select_plan_confirm_point_move_on_date,
                                wrappedList(date, transition.plan.name),
                            ),
                        )
                    }
                    if (currentPlan != null) {
                        add(
                            resourceReference(
                                R.string.tangempay_select_plan_confirm_point_cards_closed,
                                wrappedList(currentPlan.programName),
                            ),
                        )
                    }
                    if (date != null) {
                        add(
                            resourceReference(
                                R.string.tangempay_select_plan_confirm_point_cancel_till,
                                wrappedList(date),
                            ),
                        )
                    }
                    add(resourceReference(R.string.tangempay_select_plan_confirm_point_no_fee))
                }
            }
            else -> listOf(resourceReference(R.string.tangempay_select_plan_confirm_point_no_fee))
        }
            .map { TangemPaySelectPlanUM.PointUM(title = it, body = null) }
            .toImmutableList()
    }

    private fun TangemPayTariffPlan.toPlanUM() = TangemPaySelectPlanUM.PlanUM(
        name = stringReference(name),
        imageUrl = mainImageUrl,
        points = descriptionItems
            .filter { it.section == TangemPayTariffPlan.Section.ONBOARDING_RELATED }
            .sortedBy { it.order }
            .map { item ->
                TangemPaySelectPlanUM.PointUM(
                    title = stringReference(item.title),
                    body = item.body.takeIf(String::isNotBlank)?.let(::stringReference),
                )
            }
            .toImmutableList(),
    )

    private fun TangemPayCustomerTariffPlan?.nextBillingDate(): String? {
        return this?.formatNextBillingDateOrNull(DateTimeFormatters.dateMMMd)
    }

    private companion object {
        val COMPARE_SECTIONS = setOf(
            TangemPayTariffPlan.Section.CARD_RELATED,
            TangemPayTariffPlan.Section.PLAN_RELATED,
        )
    }
}