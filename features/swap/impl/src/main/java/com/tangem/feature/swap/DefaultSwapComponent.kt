package com.tangem.feature.swap

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.choosetoken.api.ChooseTokenAnalyticsPayload
import com.tangem.feature.swap.choosetoken.api.ChooseTokenComponent
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.router.SwapNavScreen
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

    private val model: SwapModel = getOrCreateModel(params)

    // todo swap create InnerRouter
    private val chooseTokenComponent by lazy {
        chooseTokenComponentFactory.create(
            context = child("chooseTokenComponent"),
            params = ChooseTokenComponent.Params(
                bridge = model.chooseTokenBridge,
                settings = ChooseTokenComponent.Settings.SwapTo,
                analyticsPayload = setOf(
                    ChooseTokenAnalyticsPayload.ScreensSources(ScreensSources.Swap.value),
                ),
            ),
        )
    }

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
    private val childSlot = childSlot(
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

    data class FeeSelectorConfig(
        val sendingCurrencyStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus,
    )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content(modifier: Modifier) {
        val dataState by model.dataStateStateFlow.collectAsStateWithLifecycle()
        val fromCryptoCurrency by remember { derivedStateOf { dataState.fromCryptoCurrency } }
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

        val feeSelectorChildStackState by childSlot.subscribeAsState()
        val feeSelectorBlockComponent = feeSelectorChildStackState.child?.instance

        Crossfade(
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            targetState = model.currentScreen,
            label = "",
        ) { screen ->
            when (screen) {
                SwapNavScreen.Main -> SwapScreen(
                    stateHolder = model.uiState,
                    feeSelectorBlockComponent = feeSelectorBlockComponent,
                )
                SwapNavScreen.Success -> {
                    val successState = model.uiState.successState
                    val feeSelectorState by model.feeSelectorRepository.state.collectAsStateWithLifecycle()
                    if (successState != null) {
                        SwapSuccessScreen(
                            state = successState,
                            feeSelectorUM = feeSelectorState,
                            onBack = model.uiState.onBackClicked,
                        )
                    } else {
                        SwapScreen(
                            stateHolder = model.uiState,
                            feeSelectorBlockComponent = feeSelectorBlockComponent,
                        )
                    }
                }
                SwapNavScreen.SelectToken -> chooseTokenComponent.Content(Modifier)
            }
        }

        val approvalSlotState by approvalSlot.subscribeAsState()
        approvalSlotState.child?.instance?.BottomSheet()
    }

    fun getApprovalParams(): GiveApprovalComponent.Params? {
        val permissionState = model.uiState.permissionState as? GiveTxPermissionState.ReadyForRequest
            ?: return null
        val fromCryptoCurrency = model.dataState.fromCryptoCurrency ?: return null
        val feeCryptoCurrency = model.dataState.feePaidCryptoCurrency ?: return null
        val providerName = model.dataState.selectedProvider?.name.orEmpty()

        return GiveApprovalComponent.Params(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = fromCryptoCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrency,
            amount = model.dataState.amount.orEmpty(),
            spenderAddress = requireNotNull(model.dataState.approveDataModel).spenderAddress,
            amountFooter = if (permissionState.isResetApproval) {
                resourceReference(R.string.update_approval_permission_subtitle)
            } else {
                resourceReference(
                    id = R.string.give_permission_swap_subtitle,
                    formatArgs = wrappedList(providerName, permissionState.currency),
                )
            },
            feeFooter = resourceReference(R.string.swap_give_permission_fee_footer),
            isResetApproval = permissionState.isResetApproval,
            isHoldToConfirm = model.isHoldToConfirmEnabled,
            callback = model.approvalCallback,
        )
    }

    private fun toBigDecimalOrZero(bigDecimalString: String?): BigDecimal {
        return bigDecimalString?.replace(",", ".")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }

    private companion object {
        const val BOTTOM_SHEET_SLOT_KEY = "bottomSheetSlot"
        const val FEE_SELECTOR_SLOT_KEY = "feeSelectorSlot"
        const val APPROVAL_SLOT_KEY = "approvalSlot"
    }
}