package com.tangem.features.send.send.confirm

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsComponent
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.impl.R
import com.tangem.features.send.send.confirm.model.SendConfirmModel
import com.tangem.features.send.send.confirm.ui.SendConfirmContent
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.features.send.subcomponents.amount.DefaultSendAmountBlockComponent
import com.tangem.features.send.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.subcomponents.notifications.DefaultSendNotificationsComponent
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.*

internal class SendConfirmComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    feeSelectorComponentFactory: FeeSelectorBlockComponent.Factory,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: SendConfirmModel = getOrCreateModel(params = params)

    private val blockClickEnableFlow = MutableStateFlow(false)

    private val destinationBlockComponent =
        DefaultSendDestinationBlockComponent(
            appComponentContext = child("sendConfirmDestinationBlock"),
            params = DestinationBlockParams(
                state = model.uiState.value.destinationUM,
                analyticsCategoryName = params.analyticsCategoryName,
                analyticsSendSource = params.analyticsSendSource,
                userWalletId = params.userWallet.walletId,
                cryptoCurrency = params.cryptoCurrencyStatus.currency,
                blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
                predefinedValues = params.predefinedValues,
            ),
            onResult = model::onDestinationResult,
            onClick = model::showEditDestination,
        )

    private val amountBlockComponent = DefaultSendAmountBlockComponent(
        appComponentContext = child("sendConfirmAmountBlock"),
        params = SendAmountComponentParams.AmountBlockParams(
            state = model.uiState.value.amountUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWallet = params.userWallet,
            appCurrency = params.appCurrency,
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
            predefinedValues = params.predefinedValues,
            userWalletId = params.userWallet.walletId,
            cryptoCurrency = params.cryptoCurrencyStatus.currency,
            cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
            isBalanceHidingFlow = params.isBalanceHidingFlow,
            analyticsSendSource = params.analyticsSendSource,
            accountFlow = params.accountFlow,
            isAccountModeFlow = params.isAccountModeFlow,
        ),
        onResult = model::onAmountResult,
        onClick = model::showEditAmount,
    )

    private val feeSelectorBlockComponent = feeSelectorComponentFactory.create(
        context = appComponentContext,
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = model.uiState.value.feeSelectorUM,
            onLoadFee = params.onLoadFee,
            onLoadFeeExtended = params.onLoadFeeExtended,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeStateConfiguration = model.feeStateConfiguration,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
            analyticsCategoryName = params.analyticsCategoryName,
            analyticsSendSource = params.analyticsSendSource,
            userWalletId = params.userWallet.walletId,
        ),
        onResult = model::onFeeResult,
    )

    private val notificationsComponent = DefaultSendNotificationsComponent(
        appComponentContext = child("sendConfirmNotifications"),
        params = SendNotificationsComponent.Params(
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            callback = model,
            notificationData = SendNotificationsComponent.Params.NotificationData(
                destinationAddress = model.confirmData.enteredDestination.orEmpty(),
                memo = model.confirmData.enteredMemo,
                amountValue = model.confirmData.enteredAmount.orZero(),
                reduceAmountBy = model.confirmData.reduceAmountBy.orZero(),
                isIgnoreReduce = model.confirmData.isIgnoreReduce,
                fee = model.confirmData.fee,
                feeError = model.confirmData.feeError,
                feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
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
        feeSelectorBlockComponent.updateState(state.feeSelectorUM)
        model.updateState(state)
    }

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            text = stringResourceSafe(R.string.common_send),
            onBackClick = {
                model.onBackClick()
                router.pop()
            },
            backIconRes = R.drawable.ic_back_24,
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val notificationState by notificationsComponent.state.collectAsStateWithLifecycle()

        SendConfirmContent(
            sendUM = state,
            destinationBlockComponent = destinationBlockComponent,
            amountBlockComponent = amountBlockComponent,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
            notificationsComponent = notificationsComponent,
            notificationsUM = notificationState,
        )
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        Column {
            val sendingFooter = (state.confirmUM as? ConfirmUM.Content)?.sendingFooter
            AnimatedVisibility(
                visible = sendingFooter != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            ) {
                SendingText(footerText = sendingFooter ?: TextReference.EMPTY)
            }
            NavigationPrimaryButton(
                primaryButton = model.primaryButtonUM(state.confirmUM),
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
            )
        }
    }

    data class Params(
        val state: SendUM,
        val analyticsCategoryName: String,
        val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        val userWallet: UserWallet,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val accountFlow: StateFlow<Account?>,
        val isAccountModeFlow: StateFlow<Boolean>,
        val appCurrency: AppCurrency,
        val callback: ModelCallback,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val predefinedValues: PredefinedValues,
        val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        val onLoadFeeExtended: suspend (CryptoCurrencyStatus?) -> Either<GetFeeError, TransactionFeeExtended>,
        val onSendTransaction: () -> Unit,
    )

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }
}