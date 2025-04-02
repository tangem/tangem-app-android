package com.tangem.features.send.v2.send

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.subscribe
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.confirm.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.model.SendModel
import com.tangem.features.send.v2.send.ui.SendContent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendComponent.Params,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : SendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<SendRoute>()

    private val innerRouter = InnerRouter<SendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val initialRoute = if (params.amount == null) {
        SendRoute.Destination(isEditMode = false)
    } else {
        SendRoute.Empty
    }
    private val currentRoute = MutableStateFlow(initialRoute)

    private val model: SendModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "sendInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = initialRoute,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createChild(
                configuration,
                childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
            )
        },
    )

    init {
        childStack.subscribe(
            lifecycle = lifecycle,
            mode = ObserveLifecycleMode.CREATE_DESTROY,
        ) { stack ->
            componentScope.launch {
                when (val activeComponent = stack.active.instance) {
                    is SendConfirmComponent -> if (currentRoute.value.isEditMode) {
                        analyticsEventHandler.send(SendAnalyticEvents.ConfirmationScreenOpened)
                        activeComponent.updateState(model.uiState.value)
                    }
                    is SendAmountComponent -> {
                        analyticsEventHandler.send(SendAnalyticEvents.AmountScreenOpened)
                        activeComponent.updateState(model.uiState.value.amountUM)
                    }
                    is SendDestinationComponent -> {
                        analyticsEventHandler.send(SendAnalyticEvents.AddressScreenOpened)
                        activeComponent.updateState(model.uiState.value.destinationUM)
                    }
                    is SendFeeComponent -> {
                        analyticsEventHandler.send(SendAnalyticEvents.FeeScreenOpened)
                    }
                }
                currentRoute.emit(stack.active.configuration)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = ::onChildBack)
        SendContent(
            state = state,
            stackState = stackState,
        )
    }

    private fun createChild(route: SendRoute, factoryContext: AppComponentContext) = when (route) {
        SendRoute.Empty -> getStubComponent()
        is SendRoute.Destination -> getDestinationComponent(factoryContext, route)
        is SendRoute.Amount -> getAmountComponent(factoryContext, route)
        is SendRoute.Fee -> getFeeComponent(factoryContext)
        SendRoute.Confirm -> getConfirmComponent(factoryContext)
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext, route: SendRoute) =
        SendDestinationComponent(
            appComponentContext = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                currentRoute = currentRoute.filterIsInstance<SendRoute.Destination>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = SendAnalyticEvents.SEND_CATEGORY,
                userWallet = model.userWallet,
                cryptoCurrency = params.currency,
                callback = model,
                isEditMode = route.isEditMode,
            ),
        )

    private fun getAmountComponent(factoryContext: AppComponentContext, route: SendRoute) = SendAmountComponent(
        appComponentContext = factoryContext,
        params = SendAmountComponentParams.AmountParams(
            state = model.uiState.value.amountUM,
            currentRoute = currentRoute.filterIsInstance<SendRoute.Amount>(),
            isBalanceHidingFlow = model.isBalanceHiddenFlow,
            analyticsCategoryName = SendAnalyticEvents.SEND_CATEGORY,
            userWallet = model.userWallet,
            appCurrency = model.appCurrency,
            cryptoCurrencyStatus = model.cryptoCurrencyStatus,
            callback = model,
            isEditMode = route.isEditMode,
            predefinedAmountValue = model.predefinedAmountValue,
        ),
    )

    private fun getFeeComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val state = model.uiState.value
        val sendAmount = (state.amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.value
        return if (sendAmount != null && destinationAddress != null) {
            SendFeeComponent(
                appComponentContext = factoryContext,
                params = SendFeeComponentParams.FeeParams(
                    state = model.uiState.value.feeUM,
                    currentRoute = currentRoute.filterIsInstance<SendRoute.Fee>(),
                    analyticsCategoryName = SendAnalyticEvents.SEND_CATEGORY,
                    userWallet = model.userWallet,
                    cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
                    appCurrency = model.appCurrency,
                    sendAmount = sendAmount,
                    destinationAddress = destinationAddress,
                    callback = model,
                ),
            )
        } else {
            getStubComponent()
        }
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext): SendConfirmComponent {
        val predefinedAmount = params.amount
        val predefinedTxId = params.transactionId
        val predefinedAddress = params.destinationAddress
        val predefinedValues =
            if (predefinedAmount != null && predefinedTxId != null && predefinedAddress != null) {
                SendConfirmComponent.Params.PredefinedValues.Content(
                    amount = predefinedAmount,
                    address = predefinedAddress,
                    tag = params.tag,
                    transactionId = predefinedTxId,
                )
            } else {
                SendConfirmComponent.Params.PredefinedValues.Empty
            }
        return SendConfirmComponent(
            appComponentContext = factoryContext,
            params = SendConfirmComponent.Params(
                state = model.uiState.value,
                userWallet = model.userWallet,
                currentRoute = currentRoute.filterIsInstance<SendRoute.Confirm>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = SendAnalyticEvents.SEND_CATEGORY,
                cryptoCurrencyStatus = model.cryptoCurrencyStatus,
                feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatus,
                appCurrency = model.appCurrency,
                callback = model,
                predefinedValues = predefinedValues,
            ),
        )
    }

    private fun getStubComponent() = ComposableContentComponent { }

    private fun onChildBack() {
        val isEmptyRoute = childStack.value.active.configuration == SendRoute.Empty
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success

        if (isEmptyRoute || isEmptyStack || isSuccess) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SendComponent.Factory {
        override fun create(context: AppComponentContext, params: SendComponent.Params): DefaultSendComponent
    }
}