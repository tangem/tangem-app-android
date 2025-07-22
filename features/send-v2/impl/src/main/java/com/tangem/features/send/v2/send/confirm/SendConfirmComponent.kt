package com.tangem.features.send.v2.send.confirm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.confirm.model.SendConfirmModel
import com.tangem.features.send.v2.send.confirm.ui.SendConfirmContent
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountBlockComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeBlockComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import com.tangem.features.send.v2.subcomponents.notifications.DefaultSendNotificationsComponent
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.*

internal class SendConfirmComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    private val feeSelectorComponentFactory: FeeSelectorBlockComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendConfirmModel = getOrCreateModel(params = params)

    private val blockClickEnableFlow = MutableStateFlow(false)

    private val destinationBlockComponent =
        DefaultSendDestinationBlockComponent(
            appComponentContext = child("sendConfirmDestinationBlock"),
            params = DestinationBlockParams(
                state = model.uiState.value.destinationUM,
                analyticsCategoryName = params.analyticsCategoryName,
                userWalletId = params.userWallet.walletId,
                cryptoCurrency = params.cryptoCurrencyStatus.currency,
                blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
                predefinedValues = params.predefinedValues,
            ),
            onResult = model::onDestinationResult,
            onClick = model::showEditDestination,
        )

    private val amountBlockComponent = SendAmountBlockComponent(
        appComponentContext = child("sendConfirmAmountBlock"),
        params = SendAmountComponentParams.AmountBlockParams(
            state = model.uiState.value.amountUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWallet = params.userWallet,
            appCurrency = params.appCurrency,
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
            predefinedValues = params.predefinedValues,
            isRedesignEnabled = model.uiState.value.isRedesignEnabled,
            userWalletId = params.userWallet.walletId,
            cryptoCurrency = params.cryptoCurrencyStatus.currency,
            cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
        ),
        onResult = model::onAmountResult,
        onClick = model::showEditAmount,
    )

    private val feeBlockComponent = SendFeeBlockComponent(
        appComponentContext = child("sendConfirmFeeBlock"),
        params = SendFeeComponentParams.FeeBlockParams(
            state = model.uiState.value.feeUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWallet = params.userWallet,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            sendAmount = model.confirmData.enteredAmount.orZero(),
            destinationAddress = model.confirmData.enteredDestination.orEmpty(),
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
            onLoadFee = params.onLoadFee,
        ),
        onResult = model::onFeeResult,
        onClick = model::showEditFee,
    )

    private val feeSelectorBlockComponent = feeSelectorComponentFactory.create(
        context = appComponentContext,
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = model.uiState.value.feeSelectorUM,
            onLoadFee = params.onLoadFee,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            suggestedFeeState = model.suggestedFeeState,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
        ),
        onResult = model::onFeeResult,
    )

    private val notificationsComponent = DefaultSendNotificationsComponent(
        appComponentContext = child("sendConfirmNotifications"),
        params = SendNotificationsComponent.Params(
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            notificationData = NotificationData(
                destinationAddress = model.confirmData.enteredDestination.orEmpty(),
                memo = model.confirmData.enteredMemo,
                amountValue = model.confirmData.enteredAmount.orZero(),
                reduceAmountBy = model.confirmData.reduceAmountBy.orZero(),
                isIgnoreReduce = model.confirmData.isIgnoreReduce,
                fee = model.confirmData.fee,
                feeError = model.confirmData.feeError,
            ),
        ),
    )

    init {
        model.uiState.onEach { state ->
            val confirmUM = state.confirmUM as? ConfirmUM.Content
            blockClickEnableFlow.value = confirmUM?.isSending == false
        }.launchIn(componentScope)
    }

    fun updateState(state: SendUM) {
        destinationBlockComponent.updateState(state.destinationUM)
        amountBlockComponent.updateState(state.amountUM)
        feeBlockComponent.updateState(state.feeUM)
        model.updateState(state)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val notificationState by notificationsComponent.state.collectAsStateWithLifecycle()

        SendConfirmContent(
            sendUM = state,
            destinationBlockComponent = destinationBlockComponent,
            amountBlockComponent = amountBlockComponent,
            feeBlockComponent = feeBlockComponent,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
            notificationsComponent = notificationsComponent,
            notificationsUM = notificationState,
        )
    }

    data class Params(
        val state: SendUM,
        val analyticsCategoryName: String,
        val userWallet: UserWallet,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val appCurrency: AppCurrency,
        val callback: ModelCallback,
        val currentRoute: Flow<CommonSendRoute>,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val predefinedValues: PredefinedValues,
        val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        val onSendTransaction: () -> Unit,
    )

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }
}