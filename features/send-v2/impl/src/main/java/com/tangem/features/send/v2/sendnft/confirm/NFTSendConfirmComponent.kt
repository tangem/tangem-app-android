package com.tangem.features.send.v2.sendnft.confirm

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
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.sendnft.confirm.model.NFTSendConfirmModel
import com.tangem.features.send.v2.sendnft.confirm.ui.NFTSendConfirmContent
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.v2.subcomponents.fee.SendFeeBlockComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import com.tangem.features.send.v2.subcomponents.notifications.DefaultSendNotificationsComponent
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

internal class NFTSendConfirmComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    nftDetailsBlockComponentFactory: NFTDetailsBlockComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: NFTSendConfirmModel = getOrCreateModel(params = params)

    private val blockClickEnableFlow = MutableStateFlow(false)

    private val destinationBlockComponent =
        DefaultSendDestinationBlockComponent(
            appComponentContext = child("NFTSendConfirmDestinationBlock"),
            params = DestinationBlockParams(
                state = model.uiState.value.destinationUM,
                analyticsCategoryName = params.analyticsCategoryName,
                userWalletId = params.userWallet.walletId,
                cryptoCurrency = params.cryptoCurrencyStatus.currency,
                blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
                predefinedValues = PredefinedValues.Empty,
            ),
            onResult = model::onDestinationResult,
            onClick = model::showEditDestination,
        )

    private val feeBlockComponent = SendFeeBlockComponent(
        appComponentContext = child("NFTSendConfirmFeeBlock"),
        params = SendFeeComponentParams.FeeBlockParams(
            state = model.uiState.value.feeUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWallet = params.userWallet,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            sendAmount = BigDecimal.ZERO,
            onLoadFee = params.onLoadFee,
            destinationAddress = model.confirmData.enteredDestination.orEmpty(),
            blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
        ),
        onResult = model::onFeeResult,
        onClick = model::showEditFee,
    )

    private val nftDetailsBlockComponent = nftDetailsBlockComponentFactory.create(
        context = child("NFTDetailsBlock"),
        params = NFTDetailsBlockComponent.Params(
            userWalletId = params.userWallet.walletId,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
        ),
    )

    private val notificationsComponent = DefaultSendNotificationsComponent(
        appComponentContext = child("NFTSendConfirmNotifications"),
        params = SendNotificationsComponent.Params(
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            notificationData = SendNotificationsComponent.Params.NotificationData(
                destinationAddress = model.confirmData.enteredDestination.orEmpty(),
                memo = model.confirmData.enteredMemo,
                amountValue = BigDecimal.ZERO,
                reduceAmountBy = BigDecimal.ZERO,
                isIgnoreReduce = false,
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

    fun updateState(state: NFTSendUM) {
        destinationBlockComponent.updateState(state.destinationUM)
        feeBlockComponent.updateState(state.feeUM)
        model.updateState(state)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val notificationState by notificationsComponent.state.collectAsStateWithLifecycle()

        NFTSendConfirmContent(
            nftSendUM = state,
            destinationBlockComponent = destinationBlockComponent,
            feeBlockComponent = feeBlockComponent,
            nftDetailsBlockComponent = nftDetailsBlockComponent,
            notificationsComponent = notificationsComponent,
            notificationsUM = notificationState,
        )
    }

    data class Params(
        val state: NFTSendUM,
        val analyticsCategoryName: String,
        val userWallet: UserWallet,
        val appCurrency: AppCurrency,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val callback: ModelCallback,
        val currentRoute: Flow<CommonSendRoute.Confirm>,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
    )

    interface ModelCallback {
        fun onResult(nftSendUM: NFTSendUM)
    }
}