package com.tangem.features.tangempay.tiers.select

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.domain.pay.usecase.SubmitTariffTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPaySelectPlanModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getTransitions: GetTangemPayTariffPlanTransitionsUseCase,
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPaySelectPlanComponent.Params>()

    private var transitions: List<TangemPayTariffPlanTransition> = emptyList()

    private val allowedTransitions: List<TangemPayTariffPlanTransition>
        get() = transitions.filter { it.type in ALLOWED_TYPES }

    private val allowedTransitionsForCompare: List<TangemPayTariffPlanTransition>
        get() = transitions.filter { it.type in ALLOWED_TYPES_FOR_COMPARE }

    private var selectedIndex: Int = 0
    private var isConfirm: Boolean = false
    private var isProcessing: Boolean = false

    val state: StateFlow<TangemPaySelectPlanUM>
        field = MutableStateFlow(buildState())

    init {
        analytics.send(TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed())
        loadTransitions()
    }

    private fun loadTransitions() {
        modelScope.launch {
            getTransitions(params.userWalletId).onRight { result ->
                transitions = result
                state.update { buildState() }
            }
        }
    }

    private fun onPlanSelected(index: Int) {
        if (index == selectedIndex) return
        selectedIndex = index
        analytics.send(TangemPayAnalyticsEvents.Tiers.TiersSwiped())
        state.update { buildState() }
    }

    private fun onSelectClick() {
        val transition = allowedTransitions.getOrNull(selectedIndex) ?: return

        val tierId = transition.plan.tierId
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlanSelectedClick(tierId))

        when (params.source) {
            TangemPaySelectPlanSource.TIERS_ONBOARDING -> applyTransition(transition)
            TangemPaySelectPlanSource.CHANGE_PLAN -> {
                analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeConfirmationScreenShowed(tierId))
                isConfirm = true
                state.update { buildState() }
            }
        }
    }

    private fun onComparePlansClick() {
        if (allowedTransitions.isEmpty()) return
        analytics.send(TangemPayAnalyticsEvents.Tiers.ComparePlansClicked())
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupShowed())
        state.update { buildState(showPlanCompare = true) }
    }

    private fun onCompareDismiss() {
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupClosed())
        state.update { buildState(showPlanCompare = false) }
    }

    fun onBackClick() {
        if (isProcessing) return
        if (isConfirm) {
            analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeCancelClicked())
            isConfirm = false
            state.update { buildState() }
        } else {
            router.pop()
        }
    }

    private fun onCloseClick() {
        if (isProcessing) return
        router.pop()
    }

    private fun onConfirmClick() {
        val transition = allowedTransitions.getOrNull(selectedIndex) ?: return
        applyTransition(transition)
    }

    private fun applyTransition(transition: TangemPayTariffPlanTransition) {
        if (isProcessing) return

        if (transition.type == TangemPayTariffPlanTransition.Type.UPGRADE) {
            analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeUpgradeClicked())
        }

        isProcessing = true
        state.update { buildState() }
        modelScope.launch {
            submitTariffTransitionUseCase(params.userWalletId, transition).fold(
                ifRight = {
                    when {
                        transition.type == TangemPayTariffPlanTransition.Type.UPGRADE -> {
                            router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails)
                        }
                        params.source == TangemPaySelectPlanSource.TIERS_ONBOARDING -> {
                            router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails)
                        }
                        else -> router.pop()
                    }
                },
                ifLeft = {
                    isProcessing = false
                    state.update { buildState() }
                    uiMessageSender.send(message = TangemPayMessagesFactory.createGenericError())
                },
            )
        }
    }

    private fun buildState(showPlanCompare: Boolean = false): TangemPaySelectPlanUM = TangemPaySelectPlanUM(
        topBarTitle = resourceReference(
            if (isConfirm) {
                R.string.tangempay_select_plan_confirm_title
            } else {
                R.string.tangempay_select_plan_title
            },
        ),
        plans = allowedTransitions.map { it.plan.toPlanUM() }.toImmutableList(),
        selectedIndex = selectedIndex,
        onPlanSelected = ::onPlanSelected,
        onBackClick = ::onBackClick,
        onCloseClick = ::onCloseClick,
        content = if (isConfirm) buildConfirmContent() else buildSelectContent(),
        compare = if (showPlanCompare) buildCompare() else null,
    )

    private fun buildSelectContent() = TangemPaySelectPlanUM.Content.Select(
        isProcessing = isProcessing,
        onComparePlansClick = ::onComparePlansClick,
        onSelectClick = ::onSelectClick,
    )

    private fun buildCompare(): TangemPaySelectPlanUM.ComparePlans {
        val plans = listOf(params.tariffPlan.plan) + allowedTransitionsForCompare.map { it.plan }
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
            onDismiss = ::onCompareDismiss,
        )
    }

    private fun buildConfirmContent(): TangemPaySelectPlanUM.Content {
        val transition = allowedTransitions.getOrNull(selectedIndex) ?: return buildSelectContent()
        val targetPlan = transition.plan
        return TangemPaySelectPlanUM.Content.Confirm(
            title = when (transition.type) {
                TangemPayTariffPlanTransition.Type.UPGRADE -> resourceReference(
                    R.string.tangempay_select_plan_confirm_upgrade_title,
                    wrappedList(targetPlan.programName),
                )
                TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                    val nextBillingDate = nextBillingDate()
                    if (nextBillingDate != null) {
                        val currentPlan = params.tariffPlan.plan
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
            points = buildConfirmPoints(transition),
            confirmButtonText = resourceReference(
                when (transition.type) {
                    TangemPayTariffPlanTransition.Type.UPGRADE -> R.string.tangempay_select_plan_btn_upgrade
                    TangemPayTariffPlanTransition.Type.DOWNGRADE -> R.string.tangempay_select_plan_btn_downgrade
                    else -> R.string.common_continue
                },
            ),
            isProcessing = isProcessing,
            onCancelClick = ::onBackClick,
            onConfirmClick = ::onConfirmClick,
        )
    }

    private fun buildConfirmPoints(
        transition: TangemPayTariffPlanTransition,
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
                val date = nextBillingDate()
                val currentPlan = params.tariffPlan.plan
                buildList {
                    if (date != null) {
                        add(
                            resourceReference(
                                R.string.tangempay_select_plan_confirm_point_move_on_date,
                                wrappedList(date, transition.plan.name),
                            ),
                        )
                    }
                    add(
                        resourceReference(
                            R.string.tangempay_select_plan_confirm_point_cards_closed,
                            wrappedList(currentPlan.programName),
                        ),
                    )
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
        imageUrl = images.firstOrNull { it.type == TangemPayTariffPlan.Image.Type.MAIN }?.url,
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

    private fun nextBillingDate(): String? {
        return params.tariffPlan.formatNextBillingDateOrNull(DateTimeFormatters.dateMMMd)
    }

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
        private val COMPARE_SECTIONS = setOf(
            TangemPayTariffPlan.Section.CARD_RELATED,
            TangemPayTariffPlan.Section.PLAN_RELATED,
        )
    }
}