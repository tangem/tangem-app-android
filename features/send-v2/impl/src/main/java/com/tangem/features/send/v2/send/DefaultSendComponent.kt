package com.tangem.features.send.v2.send

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.model.SendModel
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance

internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendComponent.Params,
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

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::onChildBack)
    }

    private fun createChild(route: SendRoute, factoryContext: AppComponentContext) = when (route) {
        SendRoute.Empty -> getStubComponent()
        is SendRoute.Destination -> getDestinationComponent(factoryContext, route)
        is SendRoute.Amount -> getAmountComponent(factoryContext, route)
        is SendRoute.Fee -> getFeeComponent(factoryContext)
        SendRoute.Confirm -> getConfirmComponent()
    }

    private fun getDestinationComponent(factoryContext: AppComponentContext, route: SendRoute) =
        SendDestinationComponent(
            appComponentContext = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                currentRoute = currentRoute.filterIsInstance<SendRoute.Destination>(),
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

    private fun getConfirmComponent() = getStubComponent() // todo

    private fun getStubComponent() = ComposableContentComponent { }

    private fun onChildBack() {
        if (childStack.value.active.configuration == SendRoute.Empty || childStack.value.backStack.isEmpty()) {
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