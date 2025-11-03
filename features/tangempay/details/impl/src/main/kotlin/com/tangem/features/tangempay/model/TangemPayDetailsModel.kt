package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.toArgb
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.TokenReceiveType
import com.tangem.domain.pay.DataForReceiveFactory
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayDetailsComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.utils.TangemPayErrorMessageFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Custom token name and icon url. Will be used only for F&F.
 */
private const val TOKEN_NAME = "USDC"
private const val TOKEN_ICON_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/usd-coin.png"

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: CardDetailsRepository,
    private val dataForReceiveFactory: DataForReceiveFactory,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: TangemPayDetailsComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayDetailsStateFactory(
        cardNumberEnd = params.config.cardNumberEnd,
        onBack = router::pop,
        onRefresh = ::onRefreshSwipe,
        onReceive = ::onClickReceive,
        onReveal = ::revealCardDetails,
        onCopy = ::copyData,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val revealCardDetailsJobHolder = JobHolder()

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        fetchBalance()
    }

    private fun onClickReceive() {
        val depositAddress = params.config.depositAddress
        if (depositAddress == null) {
            showError()
        } else {
            dataForReceiveFactory.getDataForReceive(depositAddress = depositAddress, chainId = params.config.chainId)
                .onRight {
                    val config = TokenReceiveConfig(
                        shouldShowWarning = false,
                        cryptoCurrency = it.currency,
                        userWalletId = it.walletId,
                        showMemoDisclaimer = false,
                        receiveAddress = it.receiveAddress,
                        type = TokenReceiveType.Custom(
                            tokenName = TOKEN_NAME,
                            tokenIconUrl = TOKEN_ICON_URL,
                            fallbackTint = TangemColorPalette.Black.toArgb(),
                            fallbackBackground = TangemColorPalette.Meadow.toArgb(),
                        ),
                    )
                    bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
                }
                .onLeft {
                    val messageUM = TangemPayErrorMessageFactory.createError(
                        type = TangemPayDetailsErrorType.Receive,
                        onDismiss = bottomSheetNavigation::dismiss,
                    )
                    bottomSheetNavigation.activate(TangemPayDetailsNavigation.Error(messageUM))
                }
        }
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = cardDetailsRepository.getCardBalance()
            uiState.update(DetailsBalanceTransformer(result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            hideCardDetails()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun revealCardDetails() {
        modelScope.launch {
            uiState.update(
                transformer = DetailsRevealProgressStateTransformer(onClickHide = ::hideCardDetails),
            )
            cardDetailsRepository.revealCardDetails()
                .onRight {
                    uiState.update(
                        transformer = DetailsRevealedStateTransformer(
                            details = it,
                            onClickHide = ::hideCardDetails,
                        ),
                    )
                }
                .onLeft {
                    uiState.update(transformer = DetailsHiddenStateTransformer(stateFactory))
                    showError()
                }
        }.saveIn(revealCardDetailsJobHolder)
    }

    private fun hideCardDetails() {
        modelScope.launch {
            revealCardDetailsJobHolder.cancel()
            uiState.update(transformer = DetailsHiddenStateTransformer(stateFactory))
        }
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text.filterNot { it.isWhitespace() }, isSensitive = true)
    }
    private fun showError() {
        uiMessageSender.send(
            SnackbarMessage(TextReference.Res(R.string.tangempay_card_details_error_text)),
        )
    }
}