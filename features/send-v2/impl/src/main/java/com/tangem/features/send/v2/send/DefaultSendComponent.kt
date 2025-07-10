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
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.common.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.common.ui.SendContent
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.utils.safeNextClick
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.model.SendModel
import com.tangem.features.send.v2.send.success.SendConfirmSuccessComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.fee.SendFeeBlockComponent
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

@Suppress("LargeClass")
internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendComponent.Params,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val feeSelectorComponentFactory: FeeSelectorBlockComponent.Factory,
) : SendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CommonSendRoute>()
    private val analyticCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY

    private val innerRouter = InnerRouter<CommonSendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: SendModel = getOrCreateModel(params = params, router = innerRouter)

    private val initialRoute = if (params.amount == null) {
        if (model.uiState.value.isRedesignEnabled) {
            CommonSendRoute.Amount(isEditMode = false)
        } else {
            CommonSendRoute.Destination(isEditMode = false)
        }
    } else {
        CommonSendRoute.Empty
    }

    private val currentRoute = MutableStateFlow(initialRoute)

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
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.ConfirmationScreenOpened(categoryName = analyticCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value)
                    }
                    is SendAmountComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.AmountScreenOpened(categoryName = analyticCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value.amountUM)
                    }
                    is DefaultSendDestinationComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.AddressScreenOpened(categoryName = analyticCategoryName),
                        )
                        activeComponent.updateState(model.uiState.value.destinationUM)
                    }
                    is SendFeeComponent -> {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.FeeScreenOpened(categoryName = analyticCategoryName),
                        )
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
            navigationUM = state.navigationUM,
            stackState = stackState,
        )
    }

    private fun createChild(route: CommonSendRoute, factoryContext: AppComponentContext) = when (route) {
        CommonSendRoute.Empty -> getStubComponent()
        is CommonSendRoute.Destination -> getDestinationComponent(factoryContext, route)
        is CommonSendRoute.Amount -> getAmountComponent(factoryContext, route)
        is CommonSendRoute.Fee -> getFeeComponent(factoryContext)
        is CommonSendRoute.Confirm -> getConfirmComponent(factoryContext)
        is CommonSendRoute.ConfirmSuccess -> getConfirmSuccessComponent(factoryContext)
    }

    private fun getDestinationComponent(
        factoryContext: AppComponentContext,
        route: CommonSendRoute,
    ): DefaultSendDestinationComponent = DefaultSendDestinationComponent(
        appComponentContext = factoryContext,
        params = SendDestinationComponentParams.DestinationParams(
            state = model.uiState.value.destinationUM,
            currentRoute = currentRoute.filterIsInstance<CommonSendRoute.Destination>(),
            isBalanceHidingFlow = model.isBalanceHiddenFlow,
            analyticsCategoryName = analyticCategoryName,
            title = resourceReference(R.string.send_recipient_label),
            userWalletId = params.userWalletId,
            cryptoCurrency = params.currency,
            callback = model,
            onBackClick = ::onChildBack,
            onNextClick = {
                val nextRoute = if (model.uiState.value.isRedesignEnabled) {
                    CommonSendRoute.Confirm
                } else {
                    CommonSendRoute.Amount(isEditMode = false)
                }
                innerRouter.safeNextClick(
                    currentRoute = route,
                    nextRoute = nextRoute,
                    popBack = ::onChildBack,
                    childStack = childStack,
                )
            },
        ),
    )

    private fun getAmountComponent(
        factoryContext: AppComponentContext,
        route: CommonSendRoute,
    ): ComposableContentComponent {
        return SendAmountComponent(
            appComponentContext = factoryContext,
            params = SendAmountComponentParams.AmountParams(
                state = model.uiState.value.amountUM,
                currentRoute = currentRoute.filterIsInstance<CommonSendRoute.Amount>(),
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = analyticCategoryName,
                appCurrency = model.appCurrency,
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                callback = model,
                predefinedValues = model.predefinedValues,
                onBackClick = {
                    if (route.isEditMode) {
                        onChildBack()
                    } else {
                        analyticsEventHandler.send(
                            CommonSendAnalyticEvents.CloseButtonClicked(
                                categoryName = analyticCategoryName,
                                source = SendScreenSource.Amount,
                                isFromSummary = false,
                                isValid = model.uiState.value.amountUM.isPrimaryButtonEnabled,
                            ),
                        )
                        if (model.uiState.value.isRedesignEnabled) {
                            onChildBack()
                        } else {
                            router.pop()
                        }
                    }
                },
                onNextClick = {
                    val nextRoute = if (model.uiState.value.isRedesignEnabled) {
                        CommonSendRoute.Destination(isEditMode = false)
                    } else {
                        CommonSendRoute.Confirm
                    }
                    innerRouter.safeNextClick(
                        currentRoute = route,
                        nextRoute = nextRoute,
                        popBack = ::onChildBack,
                        childStack = childStack,
                    )
                },
                isRedesignEnabled = model.uiState.value.isRedesignEnabled,
            ),
        )
    }

    @Suppress("ComplexCondition")
    private fun getFeeComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val state = model.uiState.value
        val sendAmount = (state.amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.actualAddress
        val cryptoCurrencyStatus = model.cryptoCurrencyStatusFlow.value
        val feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatusFlow.value

        return if (sendAmount != null && destinationAddress != null &&
            feeCryptoCurrencyStatus != null && cryptoCurrencyStatus != null
        ) {
            // TODO Apply new component [REDACTED_TASK_KEY]
            SendFeeComponent(
                appComponentContext = factoryContext,
                params = SendFeeComponentParams.FeeParams(
                    state = model.uiState.value.feeUM,
                    currentRoute = currentRoute.filterIsInstance<CommonSendRoute.Fee>(),
                    analyticsCategoryName = analyticCategoryName,
                    userWallet = model.userWallet,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    appCurrency = model.appCurrency,
                    onLoadFee = model::loadFee,
                    sendAmount = sendAmount,
                    destinationAddress = destinationAddress,
                    callback = model,
                    onNextClick = ::onChildBack,
                ),
            )
        } else {
            model.showAlertError()
            getStubComponent()
        }
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val cryptoCurrencyStatus = model.cryptoCurrencyStatusFlow.value
        val feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatusFlow.value

        return if (cryptoCurrencyStatus.value != CryptoCurrencyStatus.Loading &&
            feeCryptoCurrencyStatus.value != CryptoCurrencyStatus.Loading
        ) {
            SendConfirmComponent(
                appComponentContext = factoryContext,
                params = SendConfirmComponent.Params(
                    state = model.uiState.value,
                    userWallet = model.userWallet,
                    currentRoute = currentRoute,
                    isBalanceHidingFlow = model.isBalanceHiddenFlow,
                    analyticsCategoryName = analyticCategoryName,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                    feeCryptoCurrencyStatusFlow = model.feeCryptoCurrencyStatusFlow,
                    appCurrency = model.appCurrency,
                    callback = model,
                    predefinedValues = model.predefinedValues,
                    onLoadFee = model::loadFee,
                    onSendTransaction = {
                        innerRouter.replaceAll(CommonSendRoute.ConfirmSuccess)
                    },
                ),
                feeSelectorComponentFactory = feeSelectorComponentFactory,
            )
        } else {
            model.showAlertError()
            getStubComponent()
        }
    }

    private fun getConfirmSuccessComponent(factoryContext: AppComponentContext): ComposableContentComponent {
        val state = model.uiState.value
        val sendAmount = (state.amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.value
        val txUrl = (state.confirmUM as? ConfirmUM.Success)?.txUrl
        val cryptoCurrencyStatus = model.cryptoCurrencyStatusFlow.value
        val feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatusFlow.value

        if (sendAmount == null ||
            destinationAddress == null ||
            txUrl == null
        ) {
            model.showAlertError()
            return getStubComponent()
        }

        val destinationBlockComponent =
            DefaultSendDestinationBlockComponent(
                appComponentContext = child("sendConfirmDestinationBlock"),
                params = SendDestinationComponentParams.DestinationBlockParams(
                    state = model.uiState.value.destinationUM,
                    analyticsCategoryName = analyticCategoryName,
                    userWalletId = model.userWallet.walletId,
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    blockClickEnableFlow = MutableStateFlow(true),
                    predefinedValues = model.predefinedValues,
                ),
                onResult = { },
                onClick = {},
            )

        val feeBlockComponent = SendFeeBlockComponent(
            appComponentContext = child("sendConfirmFeeBlock"),
            params = SendFeeComponentParams.FeeBlockParams(
                state = model.uiState.value.feeUM,
                analyticsCategoryName = analyticCategoryName,
                userWallet = model.userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                appCurrency = model.appCurrency,
                sendAmount = sendAmount,
                destinationAddress = destinationAddress,
                blockClickEnableFlow = MutableStateFlow(true),
                onLoadFee = model::loadFee,
            ),
            onResult = { },
            onClick = {},
        )

        return SendConfirmSuccessComponent(
            appComponentContext = factoryContext,
            params = SendConfirmSuccessComponent.Params(
                sendUMFlow = model.uiState,
                feeBlockComponent = feeBlockComponent,
                destinationBlockComponent = destinationBlockComponent,
                analyticsCategoryName = analyticCategoryName,
                currentRoute = currentRoute,
                txUrl = txUrl,
                callback = model,
            ),
        )
    }

    private fun getStubComponent() = StubComponent()

    class StubComponent : ComposableContentComponent {
        @Composable
        override fun Content(modifier: Modifier) {
        }
    }

    private fun onChildBack() {
        val isEmptyRoute = childStack.value.active.configuration == CommonSendRoute.Empty
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success
        val isStubComponent = childStack.value.active.instance is StubComponent

        val isPopSend = isEmptyRoute || isEmptyStack || isSuccess || isStubComponent
        if (isPopSend) {
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