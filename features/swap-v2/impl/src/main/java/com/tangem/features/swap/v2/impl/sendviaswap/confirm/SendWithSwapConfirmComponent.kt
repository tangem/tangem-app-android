package com.tangem.features.swap.v2.impl.sendviaswap.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.swap.v2.impl.amount.SwapAmountBlockComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponentParams
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.sendviaswap.DefaultSendWithSwapComponent.Companion.SEND_WITH_SWAP_PROVIDER_TYPES
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.SendWithSwapConfirmModel
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.ui.SendWithSwapConfirmContent
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

internal class SendWithSwapConfirmComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
    sendDestinationBlockComponent: SendDestinationBlockComponent.Factory,
    feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
    sendNotificationsComponentFactory: SendNotificationsComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendWithSwapConfirmModel = getOrCreateModel(params = params)

    private val blockClickEnableFlow = MutableStateFlow(false)

    private val amountBlockComponent = SwapAmountBlockComponent(
        appComponentContext = child("sendWithSwapConfirmAmountBlock"),
        params = SwapAmountComponentParams.AmountBlockParams(
            amountUM = model.uiState.value.amountUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWallet = params.userWallet,
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
            primaryCryptoCurrencyStatusFlow = params.primaryCryptoCurrencyStatusFlow,
            secondaryCryptoCurrency = model.secondaryCurrency,
            isBalanceHidingFlow = params.isBalanceHidingFlow,
            swapDirection = params.swapDirection,
            filterProviderTypes = SEND_WITH_SWAP_PROVIDER_TYPES,
        ),
        onResult = model::onAmountResult,
        onClick = model::showEditAmount,
    )

    private val sendDestinationBlockComponent = sendDestinationBlockComponent.create(
        context = child("sendWithSwapConfirmDestinationBlock"),
        params = SendDestinationComponentParams.DestinationBlockParams(
            state = model.uiState.value.destinationUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
            cryptoCurrency = model.secondaryCurrency,
            predefinedValues = PredefinedValues.Empty,
        ),
        onResult = model::onDestinationResult,
        onClick = model::showEditDestination,
    )

    private val feeSelectorBlockComponent = feeSelectorBlockComponentFactory.create(
        context = child("sendWithSwapConfirmFeeBlock"),
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = model.uiState.value.feeSelectorUM,
            onLoadFee = model::loadFee,
            feeCryptoCurrencyStatus = model.primaryFeePaidCurrencyStatus,
            cryptoCurrencyStatus = model.primaryCurrencyStatus,
            suggestedFeeState = FeeSelectorParams.SuggestedFeeState.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
        ),
        onResult = model::onFeeResult,
    )

    private val sendNotificationsComponent = sendNotificationsComponentFactory.create(
        context = appComponentContext.childByContext(child("sendWithSwapConfirmSendNotifications")),
        params = SendNotificationsComponent.Params(
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = model.primaryCurrencyStatus,
            feeCryptoCurrencyStatus = model.primaryFeePaidCurrencyStatus,
            appCurrency = params.appCurrency,
            callback = model,
            notificationData = SendNotificationsComponent.Params.NotificationData(
                destinationAddress = model.confirmData.enteredDestination.orEmpty(),
                memo = null,
                amountValue = model.confirmData.enteredAmount.orZero(),
                reduceAmountBy = model.confirmData.reduceAmountBy.orZero(),
                isIgnoreReduce = model.confirmData.isIgnoreReduce,
                fee = model.confirmData.fee,
                feeError = model.confirmData.feeError,
            ),
        ),
    )

    private val swapNotificationsComponent = SwapNotificationsComponent(
        appComponentContext = appComponentContext.childByContext(child("sendWithSwapConfirmSwapNotifications")),
        params = SwapNotificationsComponent.Params(
            swapNotificationData = SwapNotificationsComponent.Params.SwapNotificationData(
                expressError = (model.confirmData.quote as? SwapQuoteUM.Error)?.expressError,
                fromCryptoCurrency = model.confirmData.fromCryptoCurrencyStatus?.currency,
            ),
        ),
    )

    init {
        model.uiState.onEach { state ->
            val confirmUM = state.confirmUM as? ConfirmUM.Content
            blockClickEnableFlow.value = confirmUM?.isTransactionInProcess == false
        }.launchIn(componentScope)
    }

    fun updateState(sendWithSwapUM: SendWithSwapUM) {
        amountBlockComponent.updateState(sendWithSwapUM.amountUM)
        sendDestinationBlockComponent.updateState(sendWithSwapUM.destinationUM)
        feeSelectorBlockComponent.updateState(sendWithSwapUM.feeSelectorUM)
        model.updateState(sendWithSwapUM)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val sendWithSwapUM by model.uiState.collectAsStateWithLifecycle()
        val sendNotificationsUM by sendNotificationsComponent.state.collectAsStateWithLifecycle()
        val swapNotificationsUM by swapNotificationsComponent.state.collectAsStateWithLifecycle()

        SendWithSwapConfirmContent(
            sendWithSwapUM = sendWithSwapUM,
            amountBlockComponent = amountBlockComponent,
            sendDestinationBlockComponent = sendDestinationBlockComponent,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
            sendNotificationsComponent = sendNotificationsComponent,
            sendNotificationsUM = sendNotificationsUM,
            swapNotificationsComponent = swapNotificationsComponent,
            swapNotificationsUM = swapNotificationsUM,
            modifier = modifier,
        )
    }

    data class Params(
        val sendWithSwapUM: SendWithSwapUM,
        val analyticsCategoryName: String,
        val userWallet: UserWallet,
        val appCurrency: AppCurrency,
        val currentRoute: Flow<SendWithSwapRoute>,
        val swapDirection: SwapDirection,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val primaryFeePaidCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val callback: ModelCallback,
    )

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): SendWithSwapConfirmComponent
    }

    interface ModelCallback {
        fun onResult(sendWithSwapUM: SendWithSwapUM)
    }
}