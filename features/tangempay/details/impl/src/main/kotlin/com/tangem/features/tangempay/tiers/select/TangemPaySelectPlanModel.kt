package com.tangem.features.tangempay.tiers.select

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPaySelectPlanModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getTransitions: GetTangemPayTariffPlanTransitionsUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPaySelectPlanComponent.Params>()

    private var transitions: List<TangemPayTariffPlanTransition> = emptyList()
    private var selectedIndex: Int = 0
    private var isConfirm: Boolean = false

    val state: StateFlow<TangemPaySelectPlanUM>
        field = MutableStateFlow(buildState())

    init {
        loadTransitions()
    }

    private fun loadTransitions() {
        modelScope.launch {
            getTransitions(params.userWalletId).onRight { result ->
                transitions = result.filter { it.type in ALLOWED_TYPES }
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
        if (transitions.isEmpty()) return
        isConfirm = true
        state.update { buildState() }
    }

    private fun onComparePlansClick() {
        if (transitions.isEmpty()) return
        state.update { buildState(showPlanCompare = true) }
    }

    private fun onCompareDismiss() {
        state.update { buildState(showPlanCompare = false) }
    }

    private fun onBackClick() {
        if (isConfirm) {
            isConfirm = false
            state.update { buildState() }
        } else {
            router.pop()
        }
    }

    private fun buildState(showPlanCompare: Boolean = false): TangemPaySelectPlanUM = TangemPaySelectPlanUM(
        topBarTitle = if (isConfirm) {
            resourceReference(R.string.tangempay_select_plan_confirm_title)
        } else {
            resourceReference(R.string.tangempay_select_plan_title)
        },
        plans = transitions.map { it.plan.toPlanUM() }.toImmutableList(),
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
        val plans = transitions.map { it.plan }
        val orderedTitles = plans
            .flatMap { it.descriptionItems }
            .sortedWith(compareBy({ it.section.ordinal }, { it.order }))
            .map { it.title }
            .distinct()
        return TangemPaySelectPlanUM.ComparePlans(
            attributes = orderedTitles.map(::stringReference).toImmutableList(),
            plans = plans.map { plan ->
                val valueByTitle = plan.descriptionItems.associate { it.title to it.body }
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

    private fun buildConfirmContent(): TangemPaySelectPlanUM.Content.Confirm {
        val transition = transitions[selectedIndex]
        val planName = transition.plan.name
        val isUpgrade = transition.type == TangemPayTariffPlanTransition.Type.UPGRADE
        return TangemPaySelectPlanUM.Content.Confirm(
            // TODO v_rodionov: strings hardcoded for now - wait for documentation update
            title = stringReference("We will issue Visa $planName for you"),
            points = buildConfirmPoints(transition, planName, isUpgrade),
            confirmButtonText = resourceReference(
                if (isUpgrade) {
                    R.string.tangempay_select_plan_btn_upgrade
                } else {
                    R.string.tangempay_select_plan_btn_downgrade
                },
            ),
            onCancelClick = ::onBackClick,
            onConfirmClick = {},
        )
    }

    // TODO v_rodionov: strings hardcoded for now - wait for documentation update
    private fun buildConfirmPoints(
        transition: TangemPayTariffPlanTransition,
        planName: String,
        isUpgrade: Boolean,
    ): ImmutableList<TangemPaySelectPlanUM.PointUM> = if (isUpgrade) {
        val feeText = transition.plan.descriptionItems
            .firstOrNull { it.section == TangemPayTariffPlan.Section.PLAN_RELATED }
            ?.title
        listOf(
            stringReference("You will get your virtual Visa $planName in minutes"),
            if (feeText != null) {
                stringReference("$feeText monthly fee will be taken from your account")
            } else {
                stringReference("No fee applied")
            },
        )
    } else {
        listOf(
            stringReference("Your current Visa cards will be closed"),
            stringReference("No fee applied"),
        )
    }
        .map { TangemPaySelectPlanUM.PointUM(title = it, body = null) }
        .toImmutableList()

    private fun TangemPayTariffPlan.toPlanUM() = TangemPaySelectPlanUM.PlanUM(
        name = stringReference(name),
        imageUrl = images.firstOrNull { it.type == TangemPayTariffPlan.Image.Type.MAIN }?.url,
        points = descriptionItems
            .sortedWith(compareBy({ it.section.ordinal }, { it.order }))
            .map { item ->
                TangemPaySelectPlanUM.PointUM(
                    title = stringReference(item.title),
                    body = item.body.takeIf(String::isNotBlank)?.let(::stringReference),
                )
            }
            .toImmutableList(),
    )

    companion object {
        private val ALLOWED_TYPES = setOf(
            TangemPayTariffPlanTransition.Type.UPGRADE,
            TangemPayTariffPlanTransition.Type.DOWNGRADE,
            TangemPayTariffPlanTransition.Type.ACTIVATION, // TODO v_rodionov: Only for test, must be removed in future
        )
    }
}