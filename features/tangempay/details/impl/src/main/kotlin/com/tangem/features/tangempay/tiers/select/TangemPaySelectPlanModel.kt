package com.tangem.features.tangempay.tiers.select

import androidx.compose.runtime.Stable
import arrow.core.raise.either
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.pay.usecase.GetCurrentTariffUseCase
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanTransitionsUseCase
import com.tangem.domain.pay.usecase.SubmitTariffTransitionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.components.TangemPaySelectPlanComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val getCurrentTariffUseCase: GetCurrentTariffUseCase,
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase,
    private val startTangemPayOnboardingUseCase: StartTangemPayOnboardingUseCase,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
) : Model(), TangemPaySelectPlanClickIntents {

    private val params = paramsContainer.require<TangemPaySelectPlanComponent.Params>()
    private val umFactory = TangemPaySelectPlanUMFactory(mode = params.mode, clicks = this)
    private var data = TangemPaySelectPlanData()

    val state: StateFlow<TangemPaySelectPlanUM>
        field = MutableStateFlow(umFactory.createLoading(data))

    init {
        analytics.send(TangemPayAnalyticsEvents.Tiers.TierSelectionScreenShowed())
        loadData()
    }

    private fun loadData() {
        state.value = umFactory.createLoading(data)
        modelScope.launch {
            val transitionsDeferred = async { getTransitions(params.userWalletId) }
            val tariffPlanDeferred = async { getCurrentTariffUseCase(params.userWalletId) }
            either {
                val loadedTransitions = transitionsDeferred.await().bind()
                val loadedTariffPlan = tariffPlanDeferred.await().bind()
                updateData { it.copy(transitions = loadedTransitions, tariffPlan = loadedTariffPlan) }
            }.onLeft {
                state.value = umFactory.createError(data)
            }
        }
    }

    private fun updateData(transform: (TangemPaySelectPlanData) -> TangemPaySelectPlanData) {
        data = transform(data)
        state.value = umFactory.create(data)
    }

    override fun onRetryClick() {
        loadData()
    }

    override fun onLinkClick(url: String) {
        urlOpener.openUrl(url)
    }

    override fun onPlanSelected(index: Int) {
        if (index == data.selectedIndex) return
        analytics.send(TangemPayAnalyticsEvents.Tiers.TiersSwiped())
        updateData { it.copy(selectedIndex = index) }
    }

    override fun onSelectClick() {
        val transition = data.selectedTransition ?: return

        val tierId = transition.plan.tierId
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlanSelectedClick(tierId))

        when (val mode = params.mode) {
            TangemPaySelectPlanComponent.Mode.TiersOnboarding -> applyTransition(transition)
            TangemPaySelectPlanComponent.Mode.ChangePlan -> {
                analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeConfirmationScreenShowed(tierId))
                updateData { it.copy(isConfirm = true) }
            }
            is TangemPaySelectPlanComponent.Mode.CreateOnboardingOrder -> {
                createOnboardingOrder(transition = transition, availableProducts = mode.availableProducts)
            }
        }
    }

    override fun onComparePlansClick() {
        if (data.allowedTransitions.isEmpty()) return
        analytics.send(TangemPayAnalyticsEvents.Tiers.ComparePlansClicked())
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupShowed())
        updateData { it.copy(isCompareShown = true) }
    }

    override fun onCompareDismiss() {
        analytics.send(TangemPayAnalyticsEvents.Tiers.PlansComparisonPopupClosed())
        updateData { it.copy(isCompareShown = false) }
    }

    override fun onBackClick() {
        if (data.isProcessing) return
        if (data.isConfirm) {
            analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeCancelClicked())
            updateData { it.copy(isConfirm = false) }
        } else {
            router.pop()
        }
    }

    override fun onCloseClick() {
        if (data.isProcessing) return
        router.pop()
    }

    override fun onConfirmClick() {
        val transition = data.selectedTransition ?: return
        applyTransition(transition)
    }

    private fun applyTransition(transition: TangemPayTariffPlanTransition) {
        if (data.isProcessing) return

        if (transition.type == TangemPayTariffPlanTransition.Type.UPGRADE) {
            analytics.send(TangemPayAnalyticsEvents.Tiers.PlanChangeUpgradeClicked())
        }

        updateData { it.copy(isProcessing = true) }
        modelScope.launch {
            submitTariffTransitionUseCase(params.userWalletId, transition).fold(
                ifRight = {
                    when {
                        transition.type == TangemPayTariffPlanTransition.Type.UPGRADE -> {
                            router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails)
                        }
                        params.mode == TangemPaySelectPlanComponent.Mode.TiersOnboarding -> {
                            router.replaceAll(TangemPayAccountDetailsInnerRoute.AccountDetails)
                        }
                        else -> router.pop()
                    }
                },
                ifLeft = {
                    updateData { data -> data.copy(isProcessing = false) }
                    uiMessageSender.send(message = TangemPayMessagesFactory.createGenericError())
                },
            )
        }
    }

    private fun createOnboardingOrder(
        transition: TangemPayTariffPlanTransition,
        availableProducts: Set<TangemPayProduct>,
    ) {
        if (data.isProcessing) return

        updateData { it.copy(isProcessing = true) }
        modelScope.launch {
            startTangemPayOnboardingUseCase(
                userWalletId = params.userWalletId,
                tariffPlanTransition = transition,
                availableProducts = availableProducts,
            ).onLeft { error ->
                updateData { it.copy(isProcessing = false) }
                if (error !is StartTangemPayOnboardingError.UserCancelled) {
                    uiMessageSender.send(message = TangemPayMessagesFactory.createGenericError())
                }
            }.onRight {
                router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayOnboardingSuccess)
            }
        }
    }
}