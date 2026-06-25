package com.tangem.features.send.sendnft.confirm

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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams.FeeStateConfiguration
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsComponent
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.impl.R
import com.tangem.features.send.sendnft.confirm.model.NFTSendConfirmModel
import com.tangem.features.send.sendnft.confirm.ui.NFTSendConfirmContent
import com.tangem.features.send.sendnft.ui.state.NFTSendUM
import com.tangem.features.send.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.subcomponents.notifications.DefaultSendNotificationsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

internal class NFTSendConfirmComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
    nftDetailsBlockComponentFactory: NFTDetailsBlockComponent.Factory,
    feeSelectorComponentFactory: FeeSelectorBlockComponent.Factory,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: NFTSendConfirmModel = getOrCreateModel(params = params)

    private val blockClickEnableFlow = MutableStateFlow(false)

    private val destinationBlockComponent =
        DefaultSendDestinationBlockComponent(
            appComponentContext = child("NFTSendConfirmDestinationBlock"),
            params = DestinationBlockParams(
                state = model.uiState.value.destinationUM,
                analyticsCategoryName = params.analyticsCategoryName,
                analyticsSendSource = params.analyticsSendSource,
                userWalletId = params.userWallet.walletId,
                cryptoCurrency = params.cryptoCurrencyStatus.currency,
                blockClickEnableFlow = blockClickEnableFlow.asStateFlow(),
                predefinedValues = PredefinedValues.Empty,
            ),
            onResult = model::onDestinationResult,
            onClick = model::showEditDestination,
        )

    private val feeSelectorBlockComponent = feeSelectorComponentFactory.create(
        context = child("NFTSendConfirmFeeSelectorBlock"),
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = model.uiState.value.feeSelectorUM,
            onLoadFee = params.onLoadFee,
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeStateConfiguration = FeeStateConfiguration.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
            analyticsCategoryName = params.analyticsCategoryName,
            analyticsSendSource = params.analyticsSendSource,
            userWalletId = params.userWallet.walletId,
        ),
        onResult = model::onFeeResult,
    )

    private val nftDetailsBlockComponent = nftDetailsBlockComponentFactory.create(
        context = child("NFTDetailsBlock"),
        params = NFTDetailsBlockComponent.Params(
            userWalletId = params.userWallet.walletId,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
            isSuccessScreen = false,
            account = params.account,
            isAccountsMode = params.isAccountsMode,
            walletTitle = resourceReference(R.string.send_from_wallet_name, wrappedList(params.userWallet.name)),
        ),
    )

    private val notificationsComponent = DefaultSendNotificationsComponent(
        appComponentContext = child("NFTSendConfirmNotifications"),
        params = SendNotificationsComponent.Params(
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            appCurrency = params.appCurrency,
            callback = model,
            notificationData = SendNotificationsComponent.Params.NotificationData(
                destinationAddress = model.confirmData.enteredDestination.orEmpty(),
                memo = model.confirmData.enteredMemo,
                amountValue = BigDecimal.ZERO,
                reduceAmountBy = BigDecimal.ZERO,
                isIgnoreReduce = false,
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

    fun updateState(state: NFTSendUM) {
        destinationBlockComponent.updateState(state.destinationUM)
        model.updateState(state)
    }

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            text = stringResourceSafe(R.string.nft_send),
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

        NFTSendConfirmContent(
            nftSendUM = state,
            destinationBlockComponent = destinationBlockComponent,
            feeSelectorBlockComponent = feeSelectorBlockComponent,
            nftDetailsBlockComponent = nftDetailsBlockComponent,
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
                primaryButton = model.primaryButtonUM(),
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
            )
        }
    }

    data class Params(
        val state: NFTSendUM,
        val analyticsCategoryName: String,
        val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        val userWallet: UserWallet,
        val appCurrency: AppCurrency,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val account: Account.CryptoPortfolio?,
        val isAccountsMode: Boolean,
        val callback: ModelCallback,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        val onSendTransaction: () -> Unit,
    )

    interface ModelCallback {
        fun onResult(nftSendUM: NFTSendUM)
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): NFTSendConfirmComponent
    }
}