package com.tangem.features.tangempay.verified

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.pay.usecase.GetTangemPayProductsAvailabilityUseCase
import com.tangem.features.tangempay.components.TangemPayVerifiedComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class TangemPayVerifiedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val getTangemPayProductsAvailabilityUseCase: GetTangemPayProductsAvailabilityUseCase,
    private val startTangemPayOnboardingUseCase: StartTangemPayOnboardingUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayVerifiedComponent.Params>()

    private val createOrderJobHolder = JobHolder()

    private var availableProducts: Set<TangemPayProduct> = emptySet()

    private val stateFactory = TangemPayVerifiedUMFactory(
        onChoosePlanClick = ::onChoosePlanClick,
        onOpenMoneyAccountClick = ::onOpenMoneyAccountClick,
        onNotifyClick = ::onNotifyClick,
        onCloseClick = ::onCloseClick,
    )

    val stackNavigation = StackNavigation<TangemPayVerifiedRoute>()

    val uiState: StateFlow<TangemPayVerifiedUM>
        field = MutableStateFlow<TangemPayVerifiedUM>(TangemPayVerifiedUM.Loading(onCloseClick = ::onCloseClick))

    init {
        loadProductsAvailability()
    }

    private fun loadProductsAvailability() {
        uiState.value = TangemPayVerifiedUM.Loading(onCloseClick = ::onCloseClick)
        modelScope.launch {
            getTangemPayProductsAvailabilityUseCase(userWalletId = params.userWalletId)
                .onRight { products ->
                    availableProducts = products
                    uiState.value = stateFactory.create(products)
                }
                .onLeft { showRetryDialog() }
        }
    }

    private fun showRetryDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_retry),
                        onClick = ::loadProductsAvailability,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    private fun onChoosePlanClick() {
        stackNavigation.pushNew(
            TangemPayVerifiedRoute.SelectPlan(availableProducts = availableProducts)
        )
    }

    private fun onOpenMoneyAccountClick() {
        if (createOrderJobHolder.isActive) return

        modelScope.launch {
            startTangemPayOnboardingUseCase(
                userWalletId = params.userWalletId,
                tariffPlanTransition = null,
                availableProducts = availableProducts,
            ).onLeft { error ->
                if (error !is StartTangemPayOnboardingError.UserCancelled) {
                    showErrorDialog()
                }
            }.onRight {
                // TODO v_rodiononov: #[REDACTED_TASK_KEY] Tangem Pay Onboarding Success Screen
            }
        }.saveIn(createOrderJobHolder)
    }

    private fun showErrorDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
            ),
        )
    }

    private fun onNotifyClick() {
        // TODO v_rodionov: #[REDACTED_TASK_KEY]
    }

    private fun onCloseClick() {
        router.pop()
    }
}