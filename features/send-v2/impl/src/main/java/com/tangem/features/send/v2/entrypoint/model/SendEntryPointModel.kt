package com.tangem.features.send.v2.entrypoint.model

import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.notifications.ShouldShowNotificationUseCase
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.entrypoint.SendEntryRoute
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateTrigger
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@ModelScoped
internal class SendEntryPointModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    val router: Router,
    private val sendAmountUpdateTrigger: SendAmountUpdateTrigger,
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger,
    private val shouldShowNotificationUseCase: ShouldShowNotificationUseCase,
) : Model(),
    SendComponent.ModelCallback,
    SendWithSwapComponent.ModelCallback,
    ChooseManagedTokensComponent.ModelCallback {

    private var lastSavedAmount = ""

    override fun onConvertToAnotherToken(lastAmount: String) {
        lastSavedAmount = lastAmount
        modelScope.launch {
            val showSendViaSwapNotification = shouldShowNotificationUseCase(
                NotificationId.SendViaSwapTokenSelectorNotification.key,
            )
            router.push(
                SendEntryRoute.ChooseToken(
                    showSendViaSwapNotification = showSendViaSwapNotification,
                ),
            )
        }
    }

    override fun onCloseSwap(lastAmount: String) {
        lastSavedAmount = lastAmount
        modelScope.launch {
            if (lastAmount.isNotBlank()) {
                sendAmountUpdateTrigger.triggerUpdateAmount(lastAmount)
            }
            router.replaceAll(SendEntryRoute.Send)
        }
    }

    @Suppress("MagicNumber")
    override fun onResult() {
        modelScope.launch {
            if (lastSavedAmount.isNotBlank()) {
                swapAmountUpdateTrigger.triggerUpdateAmount(lastSavedAmount)
            }
            // Workaround in order to execute correct exit animation on SendEntryRoute.ChooseToken
            router.pop()
            delay(10L)
            router.replaceAll(SendEntryRoute.SendWithSwap)
        }
    }

    override fun onBack() {
        router.pop()
    }
}