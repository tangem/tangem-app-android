package com.tangem.features.tangempay.tiers.select

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.pay.usecase.CreateTariffPlanTransitionOrderUseCase
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.domain.pay.usecase.SetTariffPlanPendingTransitionUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.tiers.formatNextBillingDateOrNull
import com.tangem.features.tangempay.tiers.formatRecurringFeeOrNull
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
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
    private val createTransitionOrder: CreateTariffPlanTransitionOrderUseCase,
    private val setPendingTransition: SetTariffPlanPendingTransitionUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<TangemPaySelectPlanComponent.Params>()

    private var transitions: List<TangemPayTariffPlanTransition> = emptyList()

    private val allowedTransitions: List<TangemPayTariffPlanTransition>
        get() = transitions.filter { it.type in ALLOWED_TYPES }

    private var selectedIndex: Int = 0
    private var isConfirm: Boolean = false
    private var isProcessing: Boolean = false

    val state: StateFlow<TangemPaySelectPlanUM>
        field = MutableStateFlow(buildState())

    init {
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
        state.update { buildState() }
    }

    private fun onSelectClick() {
        if (allowedTransitions.isEmpty()) return
        isConfirm = true
        state.update { buildState() }
    }

    private fun onComparePlansClick() {
        if (allowedTransitions.isEmpty()) return
        state.update { buildState(showPlanCompare = true) }
    }

    private fun onCompareDismiss() {
        state.update { buildState(showPlanCompare = false) }
    }

    private fun onBackClick() {
        if (isProcessing) return
        if (isConfirm) {
            isConfirm = false
            state.update { buildState() }
        } else {
            router.pop()
        }
    }

    private fun onConfirmClick() {
        if (isProcessing) return

        val transition = allowedTransitions.getOrNull(selectedIndex) ?: return
        when (transition.type) {
            TangemPayTariffPlanTransition.Type.ACTIVATION,
            TangemPayTariffPlanTransition.Type.UPGRADE,
            -> submitTransition {
                createTransitionOrder(
                    userWalletId = params.userWalletId,
                    targetTariffPlanId = transition.plan.id,
                    transitionType = transition.type,
                )
            }
            TangemPayTariffPlanTransition.Type.DOWNGRADE -> submitTransition {
                setPendingTransition(
                    userWalletId = params.userWalletId,
                    pendingTariffPlanId = transition.plan.id,
                )
            }
            else -> Unit
        }
    }

    private fun submitTransition(action: suspend () -> Either<VisaApiError, Unit>) {
        isProcessing = true
        state.update { buildState() }
        modelScope.launch {
            action().fold(
                ifRight = { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails) },
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
        onCloseClick = router::pop,
        content = if (isConfirm) buildConfirmContent() else buildSelectContent(),
        compare = if (showPlanCompare) buildCompare() else null,
    )

    private fun buildSelectContent() = TangemPaySelectPlanUM.Content.Select(
        onComparePlansClick = ::onComparePlansClick,
        onSelectClick = ::onSelectClick,
    )

    private fun buildCompare(): TangemPaySelectPlanUM.ComparePlans {
        val plans = listOf(params.tariffPlan.plan) + transitions.map { it.plan }
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

    // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
    private fun buildConfirmContent(): TangemPaySelectPlanUM.Content {
        val transition = allowedTransitions.getOrNull(selectedIndex) ?: return buildSelectContent()
        val targetProgramme = transition.plan.name
        return TangemPaySelectPlanUM.Content.Confirm(
            title = when (transition.type) {
                TangemPayTariffPlanTransition.Type.UPGRADE -> stringReference(
                    "We will issue Visa $targetProgramme for you",
                )
                TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                    val nextBillingDate = nextBillingDate()
                    if (nextBillingDate != null) {
                        val programName = "UNKNOWN" // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
                        val planName = params.tariffPlan.plan.name
                        stringReference(
                            "Your $planName plan and $programName cards will be active till $nextBillingDate",
                        )
                    } else {
                        stringReference("You are switching to $targetProgramme")
                    }
                }
                else -> stringReference("You are switching to $targetProgramme")
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

    // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
    private fun buildConfirmPoints(
        transition: TangemPayTariffPlanTransition,
    ): ImmutableList<TangemPaySelectPlanUM.PointUM> {
        val programName = "UNKNOWN" // TODO v_rodionov: #[REDACTED_TASK_KEY] fix hardcoded strings
        return when (transition.type) {
            TangemPayTariffPlanTransition.Type.UPGRADE -> {
                val feeText = transition.plan.formatRecurringFeeOrNull()
                buildList {
                    add("You will get your virtual Visa $programName in minutes")
                    if (feeText != null) {
                        add("$feeText monthly fee will be taken from your account")
                    }
                }
            }
            TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                val date = nextBillingDate()
                buildList {
                    if (date != null) {
                        add("On $date we will move you to ${transition.plan.name} plan")
                    }
                    add("Your Visa $programName cards will be closed")
                    if (date != null) {
                        add("You can cancel this transition till $date")
                    }
                    add("No fee applied")
                }
            }
            else -> listOf("No fee applied")
        }
            .map { TangemPaySelectPlanUM.PointUM(title = stringReference(it), body = null) }
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
        private val COMPARE_SECTIONS = setOf(
            TangemPayTariffPlan.Section.CARD_RELATED,
            TangemPayTariffPlan.Section.PLAN_RELATED,
        )
    }
}