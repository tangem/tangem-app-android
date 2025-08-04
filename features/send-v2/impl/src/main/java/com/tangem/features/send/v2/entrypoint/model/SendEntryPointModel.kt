package com.tangem.features.send.v2.entrypoint.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.notifications.ShouldShowNotificationUseCase
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.SendEntryPointComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateTrigger
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkListener
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import jakarta.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@ModelScoped
internal class SendEntryPointModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    val appRouter: AppRouter,
    private val swapChooseTokenNetworkListener: SwapChooseTokenNetworkListener,
    private val sendAmountUpdateTrigger: SendAmountUpdateTrigger,
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger,
    private val shouldShowNotificationUseCase: ShouldShowNotificationUseCase,
) : Model(), SendComponent.ModelCallback, SendWithSwapComponent.ModelCallback {

    private val params: SendEntryPointComponent.Params = paramsContainer.require()

    val sendEntryPointState: StateFlow<SendEntryPoint>
    field = MutableStateFlow(SendEntryPoint.SendVanilla)

    private var swapChooseTokenListenerJobHolder = JobHolder()

    override fun onConvertToAnotherToken(lastAmount: String) {
        modelScope.launch {
            val showSendViaSwapNotification = shouldShowNotificationUseCase(
                NotificationId.SendViaSwapTokenSelectorNotification.key,
            )
            appRouter.push(
                AppRoute.ChooseManagedTokens(
                    userWalletId = params.userWalletId,
                    initialCurrency = params.cryptoCurrency,
                    selectedCurrency = null,
                    source = AppRoute.ChooseManagedTokens.Source.SendViaSwap,
                    showSendViaSwapNotification = showSendViaSwapNotification,
                ),
            )
            observeChooseSelectToken(lastAmount)
        }
    }

    override fun onCloseSwap(lastAmount: String) {
        modelScope.launch {
            if (lastAmount.isNotBlank()) {
                sendAmountUpdateTrigger.triggerUpdateAmount(lastAmount)
            }
            triggerScreenUpdate(SendEntryPoint.SendVanilla)
        }
    }

    private fun observeChooseSelectToken(lastAmount: String) {
        swapChooseTokenNetworkListener.swapChooseTokenNetworkResultFlow
            .onEach { currency ->
                if (lastAmount.isNotBlank()) {
                    swapAmountUpdateTrigger.triggerUpdateAmount(lastAmount)
                }
                triggerScreenUpdate(SendEntryPoint.SendWithSwap)
            }
            .launchIn(modelScope)
            .saveIn(swapChooseTokenListenerJobHolder)
    }

    private fun triggerScreenUpdate(entry: SendEntryPoint) {
        swapChooseTokenListenerJobHolder.cancel()
        sendEntryPointState.update { entry }
    }
}

enum class SendEntryPoint {
    SendVanilla,
    SendWithSwap,
}