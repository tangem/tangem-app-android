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
import com.tangem.core.ui.R
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.feature.swap.choosetoken.api.ChooseTokenComponent
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.models.SwapPermissionUM
import com.tangem.feature.swap.router.SwapRoute
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.swap.SwapComponent
import com.tangem.utils.extensions.isZero
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.math.BigDecimal

@Suppress("UnusedPrivateMember")
internal class DefaultSwapComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SwapComponent.Params,
    private val swapFeeSelectorBlockComponentFactory: SwapFeeSelectorBlockComponent.Factory,
    private val giveApprovalComponentFactory: GiveApprovalComponent.Factory,
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
        childFactory = { _, factoryContext ->
            val approvalParams = getApprovalParams()
                ?: error("Approval params are not available")
            giveApprovalComponentFactory.create(
                context = childByContext(factoryContext),
                params = approvalParams,
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
    )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content(modifier: Modifier) {
        val dataState by model.dataStateStateFlow.collectAsStateWithLifecycle()
        val fromCryptoCurrency by remember { derivedStateOf { dataState.fromSwapCurrencyStatus?.status } }
        val feePaidCryptoCurrency by remember { derivedStateOf { dataState.feePaidCryptoCurrency } }
        val shouldHideBlock by remember {
            derivedStateOf { toBigDecimalOrZero(dataState.amount).isZero() || model.uiState.isInsufficientFunds }
        }

        LaunchedEffect(fromCryptoCurrency, feePaidCryptoCurrency, shouldHideBlock) {
            if (shouldHideBlock) {
                TangemLogger.e(
                    "Dismissing fee selector: " +
                        "shouldHideBlock = $shouldHideBlock, amount = ${dataState.amount}, " +
                        "isInsufficientFunds = ${model.uiState.isInsufficientFunds}",
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

    private fun getApprovalParams(): GiveApprovalComponent.Params? {
        val permissionState = model.uiState.permissionUM as? SwapPermissionUM.PermissionRequired ?: return null
        val fromSwapCurrencyStatus = model.dataState.fromSwapCurrencyStatus ?: return null
        val feeCryptoCurrency = model.dataState.feePaidCryptoCurrency ?: return null
        val providerName = model.dataState.selectedProvider?.name.orEmpty()
        val isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet

        return GiveApprovalComponent.Params(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            feeCryptoCurrencyStatus = feeCryptoCurrency,
            amount = model.dataState.amount.orEmpty(),
            spenderAddress = permissionState.spenderAddress,
            amountFooter = if (permissionState.isResetApproval) {
                resourceReference(R.string.update_approval_permission_subtitle)
            } else {
                resourceReference(
                    id = R.string.give_permission_swap_subtitle,
                    formatArgs = wrappedList(providerName, fromSwapCurrencyStatus.currency.symbol),
                )
            },
            feeFooter = resourceReference(R.string.swap_give_permission_fee_footer),
            isResetApproval = permissionState.isResetApproval,
            isHoldToConfirm = isHoldToConfirm,
            callback = model.approvalCallback,
        )
    }

    private fun toBigDecimalOrZero(bigDecimalString: String?): BigDecimal {
        return bigDecimalString?.replace(",", ".")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
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