package com.tangem.features.send.v2.entrypoint.model

import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.notifications.ShouldShowNotificationUseCase
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entry.SendEntryRoute
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateTrigger
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@ModelScoped
internal class SendEntryPointModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    val router: Router,
    private val sendAmountUpdateTrigger: SendAmountUpdateTrigger,
    private val swapAmountUpdateTrigger: SwapAmountUpdateTrigger,
    private val shouldShowNotificationUseCase: ShouldShowNotificationUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(),
    SendComponent.ModelCallback,
    SendWithSwapComponent.ModelCallback,
    ChooseManagedTokensComponent.ModelCallback {

    private var lastSavedAmount = ""
    private var isEnterInFiat = false

    val currentRoute = MutableStateFlow<SendEntryRoute>(SendEntryRoute.Send)

    override fun onConvertToAnotherToken(lastAmount: String, isEnterInFiatSelected: Boolean) {
        lastSavedAmount = lastAmount
        isEnterInFiat = isEnterInFiatSelected
        modelScope.launch {
            val isShowSendViaSwapNotification = shouldShowNotificationUseCase(
                NotificationId.SendViaSwapTokenSelectorNotification.key,
            )
            router.push(
                SendEntryRoute.ChooseToken(
                    isShowSendViaSwapNotification = isShowSendViaSwapNotification,
                ),
            )
        }
    }

    override fun onCloseSwap(lastAmount: String, isEnterInFiatSelected: Boolean) {
        lastSavedAmount = lastAmount
        isEnterInFiat = isEnterInFiatSelected
        modelScope.launch {
            sendAmountUpdateTrigger.triggerUpdateAmount(lastAmount, isEnterInFiat)
            router.replaceAll(SendEntryRoute.Send)
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.AmountScreenOpened(
                    categoryName = CommonSendAnalyticEvents.SEND_CATEGORY,
                    source = CommonSendAnalyticEvents.CommonSendSource.Send,
                ),
            )
        }
    }

    @Suppress("MagicNumber")
    override fun onResult() {
        modelScope.launch {
            swapAmountUpdateTrigger.triggerUpdateAmount(lastSavedAmount, isEnterInFiat)
            // Workaround in order to execute correct exit animation on SendEntryRoute.ChooseToken
            router.pop()
            delay(10L)
            router.replaceAll(SendEntryRoute.SendWithSwap)
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.AmountScreenOpened(
                    categoryName = CommonSendAnalyticEvents.SEND_CATEGORY,
                    source = CommonSendAnalyticEvents.CommonSendSource.SendWithSwap,
                ),
            )
        }
    }

    override fun onBack() {
        modelScope.launch {
            sendAmountUpdateTrigger.triggerUpdateAmount(lastSavedAmount, isEnterInFiat)
            router.pop()
        }
    }
}