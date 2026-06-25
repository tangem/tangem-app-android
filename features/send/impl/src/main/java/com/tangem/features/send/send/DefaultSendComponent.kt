package com.tangem.features.send.send

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.decompose.EmptyComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.derivationIndex
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.amount.AmountRoute
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.api.subcomponents.destination.DestinationRoute
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.common.ui.SendModularContent
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.impl.R
import com.tangem.features.send.send.confirm.SendConfirmComponent
import com.tangem.features.send.send.model.SendModel
import com.tangem.features.send.send.success.SendConfirmSuccessComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendComponent.Params,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val amountComponentFactory: SendAmountComponent.Factory,
    private val destinationComponentFactory: SendDestinationComponent.Factory,
    private val sendConfirmSuccessComponent: SendConfirmSuccessComponent.Factory,
    private val feeSelectorComponentFactory: FeeSelectorBlockComponent.Factory,
) : SendComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CommonSendRoute>()

    private val innerRouter = InnerRouter<CommonSendRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: SendModel = getOrCreateModel(params = params, router = innerRouter)

    private val childStack = childStack(
        key = "sendInnerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.initialRoute,
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
            when (val activeComponent = stack.active.instance) {
                is SendConfirmComponent -> {
                    val fromCurrency = params.currency
                    val fromDerivationIndex = model.accountFlow.value?.derivationIndex?.value
                        .takeIf { model.isAccountModeFlow.value }
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.ConfirmationScreenOpened(
                            categoryName = model.analyticCategoryName,
                            source = model.analyticsSendSource,
                            sendBlockchain = fromCurrency.network.name,
                            sendToken = fromCurrency.symbol,
                            fromDerivationIndex = fromDerivationIndex,
                            toDerivationIndex = null,
                            type = model.consumeEntryType(),
                        ),
                    )
                    if (childStack.value.active.configuration.isEditMode) {
                        activeComponent.updateState(model.uiState.value)
                    }
                }
                is SendAmountComponent -> {
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.AmountScreenOpened(
                            categoryName = model.analyticCategoryName,
                            source = model.analyticsSendSource,
                            type = model.consumeEntryType(),
                        ),
                    )
                    activeComponent.updateState(model.uiState.value.amountUM)
                }
                is SendDestinationComponent -> {
                    analyticsEventHandler.send(
                        CommonSendAnalyticEvents.AddressScreenOpened(
                            categoryName = model.analyticCategoryName,
                            source = model.analyticsSendSource,
                        ),
                    )
                    activeComponent.updateState(model.uiState.value.destinationUM)
                }
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        SendModularContent(stackState = stackState)
    }

    private fun createChild(
        route: CommonSendRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent = when (route) {
        CommonSendRoute.Empty -> ComposableModularContentComponent.EMPTY
        is CommonSendRoute.Destination -> getDestinationComponent(route, factoryContext)
        is CommonSendRoute.Amount -> getAmountComponent(route, factoryContext)
        is CommonSendRoute.Confirm -> getConfirmComponent(factoryContext)
        is CommonSendRoute.ConfirmSuccess -> getConfirmSuccessComponent(factoryContext)
    }

    private fun getDestinationComponent(
        route: DestinationRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent {
        return destinationComponentFactory.create(
            context = factoryContext,
            params = SendDestinationComponentParams.DestinationParams(
                state = model.uiState.value.destinationUM,
                route = route,
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = model.analyticCategoryName,
                analyticsSendSource = model.analyticsSendSource,
                title = resourceReference(R.string.common_address),
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
                callback = model,
            ),
        )
    }

    private fun getAmountComponent(
        route: AmountRoute,
        factoryContext: AppComponentContext,
    ): ComposableModularContentComponent {
        return amountComponentFactory.create(
            context = factoryContext,
            params = SendAmountComponentParams.AmountParams(
                state = model.uiState.value.amountUM,
                route = route,
                isBalanceHidingFlow = model.isBalanceHiddenFlow,
                analyticsCategoryName = model.analyticCategoryName,
                appCurrency = model.appCurrency,
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                accountFlow = model.accountFlow,
                isAccountModeFlow = model.isAccountModeFlow,
                callback = model,
                predefinedValues = model.predefinedValues,
                analyticsSendSource = model.analyticsSendSource,
            ),
        )
    }

    private fun getConfirmComponent(factoryContext: AppComponentContext): ComposableModularContentComponent {
        return if (model.isAvailableForSend) {
            val cryptoCurrencyStatus = model.cryptoCurrencyStatusFlow.value
            val feeCryptoCurrencyStatus = model.feeCryptoCurrencyStatusFlow.value
            SendConfirmComponent(
                appComponentContext = factoryContext,
                params = SendConfirmComponent.Params(
                    state = model.uiState.value,
                    userWallet = model.userWallet,
                    isBalanceHidingFlow = model.isBalanceHiddenFlow,
                    analyticsCategoryName = model.analyticCategoryName,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                    feeCryptoCurrencyStatusFlow = model.feeCryptoCurrencyStatusFlow,
                    accountFlow = model.accountFlow,
                    isAccountModeFlow = model.isAccountModeFlow,
                    appCurrency = model.appCurrency,
                    callback = model,
                    predefinedValues = model.predefinedValues,
                    onLoadFee = model::loadFee,
                    onLoadFeeExtended = model::loadFeeExtended,
                    onSendTransaction = {
                        innerRouter.replaceAll(CommonSendRoute.ConfirmSuccess)
                    },
                    analyticsSendSource = model.analyticsSendSource,
                ),
                feeSelectorComponentFactory = feeSelectorComponentFactory,
            )
        } else {
            model.showAlertError()
            ComposableModularContentComponent.EMPTY
        }
    }

    private fun getConfirmSuccessComponent(factoryContext: AppComponentContext): ComposableModularContentComponent {
        val state = model.uiState.value
        val sendAmount = (state.amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
        val destinationAddress = (state.destinationUM as? DestinationUM.Content)?.addressTextField?.value
        val txUrl = (state.confirmUM as? ConfirmUM.Success)?.txUrl

        if (sendAmount == null ||
            destinationAddress == null ||
            txUrl == null
        ) {
            model.showAlertError()
            return ComposableModularContentComponent.EMPTY
        }

        return sendConfirmSuccessComponent.create(
            appComponentContext = factoryContext,
            params = SendConfirmSuccessComponent.Params(
                sendUMFlow = model.uiState,
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
                predefinedValues = model.predefinedValues,
                analyticsCategoryName = model.analyticCategoryName,
                txUrl = txUrl,
                callback = model,
            ),
        )
    }

    private fun onChildBack() {
        val isEmptyRoute = childStack.value.active.configuration == CommonSendRoute.Empty
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.value.confirmUM is ConfirmUM.Success
        val isStubComponent = childStack.value.active.instance == EmptyComposableBottomSheetComponent
        val isSendingInProgress = (model.uiState.value.confirmUM as? ConfirmUM.Content)?.isSending == true

        val isPopSend = isEmptyRoute || isEmptyStack || isSuccess || isStubComponent
        when {
            isSendingInProgress -> Unit // Do not anything while transaction sending in progress
            isPopSend -> router.pop()
            else -> stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SendComponent.Factory {
        override fun create(context: AppComponentContext, params: SendComponent.Params): DefaultSendComponent
    }
}