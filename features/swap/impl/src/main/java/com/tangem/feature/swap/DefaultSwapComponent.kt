package com.tangem.feature.swap

import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.router.SwapRoute
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.features.approval.api.GiveApprovalEntryComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.swap.SwapComponent
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultSwapComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SwapComponent.Params,
    private val swapFeeSelectorBlockComponentFactory: SwapFeeSelectorBlockComponent.Factory,
    private val giveApprovalEntryComponentFactory: GiveApprovalEntryComponent.Factory,
    private val chooseTokenComponentFactory: ChooseTokenComponent.Factory,
) : SwapComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<SwapRoute>()
    private val innerRouter = InnerRouter<SwapRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: SwapModel = getOrCreateModel(params, router = innerRouter)

    private val childStack = childStack(
        key = STACK_KEY,
        source = stackNavigation,
        serializer = null,
        initialConfiguration = SwapRoute.Main,
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(route, childByContext(factoryContext))
        },
    )

    private val approvalSlot = childSlot(
        key = APPROVAL_SLOT_KEY,
        source = model.approvalSlotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { params, factoryContext ->
            giveApprovalEntryComponentFactory.create(
                context = childByContext(factoryContext),
                params = GiveApprovalEntryComponent.Params(params),
            )
        },
    )

    init {
        lifecycle.subscribe(
            onStart = model::onStart,
            onStop = model::onStop,
        )
    }

    private val slotNavigation = SlotNavigation<FeeSelectorConfig>()
    private val feeSelectorSlot = childSlot(
        source = slotNavigation,
        serializer = null,
        key = FEE_SELECTOR_SLOT_KEY,
        childFactory = { config, context ->
            createSwapFeeSelectorBlockComponent(
                context = childByContext(context),
                config = config,
            )
        },
    )

    private fun createSwapFeeSelectorBlockComponent(
        context: AppComponentContext,
        config: FeeSelectorConfig,
    ): SwapFeeSelectorBlockComponent {
        return swapFeeSelectorBlockComponentFactory.create(
            context = context,
            params = SwapFeeSelectorBlockComponent.Params(
                repository = model.feeSelectorRepository,
                userWalletId = params.userWalletId,
                sendingCryptoCurrencyStatus = config.sendingCurrencyStatus,
                feeCryptoCurrencyStatus = config.feeCurrencyStatus,
                analyticsParams = SwapFeeSelectorBlockComponent.AnalyticsParams(
                    analyticsCategoryName = CommonSendAnalyticEvents.SWAP_CATEGORY,
                    analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Swap,
                ),
                isTransferMode = config.isTransferMode,
            ),
        )
    }

    private fun createChild(route: SwapRoute, factoryContext: AppComponentContext): ComposableContentComponent =
        when (route) {
            is SwapRoute.Main -> SwapMainChild()
            is SwapRoute.Success -> SwapSuccessChild()
            is SwapRoute.SelectToken -> {
                val bridge = if (route.isFromDirection) model.chooseFromTokenBridge else model.chooseToTokenBridge
                chooseTokenComponentFactory.create(
                    context = factoryContext,
                    params = ChooseTokenComponent.Params(bridge = bridge),
                )
            }
        }

    data class FeeSelectorConfig(
        val sendingCurrencyStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus,
        val isTransferMode: Boolean,
    )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content(modifier: Modifier) {
        val dataState by model.dataStateStateFlow.collectAsStateWithLifecycle()
        val fromCryptoCurrency by remember { derivedStateOf { dataState.fromSwapCurrencyStatus?.status } }
        val feePaidCryptoCurrency by remember { derivedStateOf { dataState.feePaidCryptoCurrency } }
        val isInTransferMode by remember { derivedStateOf { dataState.currentTransferState != null } }
        val shouldHideBlock by remember {
            derivedStateOf {
                val isAmountEmptyOrZero = dataState.amount?.parseBigDecimalOrNull().isNullOrZero()
                val isInsufficientFunds = model.uiState.isInsufficientFunds
                val isProviderMissing = dataState.selectedProvider == null
                val isPermissionNotNeeded = model.isPermissionNotNeeded
                val isSwapNotReady = !isInTransferMode && (isProviderMissing || !isPermissionNotNeeded)
                val isTangemPayWithdrawal = model.isTangemPayWithdrawal()

                isAmountEmptyOrZero || isInsufficientFunds || isSwapNotReady || isTangemPayWithdrawal
            }
        }

        LaunchedEffect(fromCryptoCurrency, feePaidCryptoCurrency, shouldHideBlock, isInTransferMode) {
            if (shouldHideBlock) {
                TangemLogger.e(
                    messageString = "Dismissing fee selector: " +
                        "shouldHideBlock = $shouldHideBlock, amount = ${dataState.amount}, " +
                        "isInsufficientFunds = ${model.uiState.isInsufficientFunds}",
                    shouldSanitize = false,
                )
                slotNavigation.dismiss()
                return@LaunchedEffect
            }

            val sendingCryptoCurrencyStatus = fromCryptoCurrency ?: run {
                TangemLogger.e("Dismissing fee selector: fromCryptoCurrency is null")
                slotNavigation.dismiss()
                return@LaunchedEffect
            }

            val feeCurrencyStatus = feePaidCryptoCurrency ?: run {
                TangemLogger.e("Dismissing fee selector: feePaidCryptoCurrency is null")
                slotNavigation.dismiss()
                return@LaunchedEffect
            }

            slotNavigation.activate(
                FeeSelectorConfig(
                    sendingCurrencyStatus = sendingCryptoCurrencyStatus,
                    feeCurrencyStatus = feeCurrencyStatus,
                    isTransferMode = isInTransferMode,
                ),
            )
        }

        val stackState by childStack.subscribeAsState()

        Children(
            stack = stackState,
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            animation = stackAnimation { fade() },
        ) { child ->
            child.instance.Content(Modifier)
        }

        val approvalSlotState by approvalSlot.subscribeAsState()
        approvalSlotState.child?.instance?.BottomSheet()
    }

    private inner class SwapMainChild : ComposableContentComponent {
        @Composable
        override fun Content(modifier: Modifier) {
            val feeSelectorChildState by feeSelectorSlot.subscribeAsState()
            val feeSelectorBlockComponent = feeSelectorChildState.child?.instance
            SwapScreen(
                stateHolder = model.uiState,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
            )
        }
    }

    private inner class SwapSuccessChild : ComposableContentComponent {
        @Composable
        override fun Content(modifier: Modifier) {
            val successState = model.uiState.successState
            val feeSelectorState by model.feeSelectorRepository.state.collectAsStateWithLifecycle()
            if (successState != null) {
                SwapSuccessScreen(
                    state = successState,
                    feeSelectorUM = feeSelectorState,
                    onBack = router::pop,
                )
            } else {
                val feeSelectorChildState by feeSelectorSlot.subscribeAsState()
                val feeSelectorBlockComponent = feeSelectorChildState.child?.instance
                SwapScreen(
                    stateHolder = model.uiState,
                    feeSelectorBlockComponent = feeSelectorBlockComponent,
                )
            }
        }
    }

    private fun onChildBack() {
        val isEmptyStack = childStack.value.backStack.isEmpty()
        val isSuccess = model.uiState.successState != null

        val isPopSend = isEmptyStack || isSuccess
        when {
            isPopSend -> router.pop()
            else -> stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }

    private companion object {
        const val STACK_KEY = "swapStack"
        const val FEE_SELECTOR_SLOT_KEY = "feeSelectorSlot"
        const val APPROVAL_SLOT_KEY = "approvalSlot"
    }
}