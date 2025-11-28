package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.pay.TangemPayTopUpData
import com.tangem.domain.pay.TangemPaySwapDataFactory
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUpdateListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
    private val tangemPaySwapDataFactory: TangemPaySwapDataFactory,
    private val orderRepository: CustomerOrderRepository,
) : Model(), TangemPayTxHistoryUiActions, TangemPayDetailIntents, AddFundsListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val cardFrozenStateConverter = TangemPayCardFrozenStateConverter(onUnfreezeClick = ::onClickUnfreezeCard)
    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        intents = this,
        cardFrozenState = params.config.cardFrozenState,
        converter = cardFrozenStateConverter,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()

    private var balance: TangemPayCardBalance? = null

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        handleBalanceHiding()
        fetchAddToWalletBanner()
        fetchBalance()
        subscribeToCardFrozenState()
    }

    private fun subscribeToCardFrozenState() {
        cardDetailsRepository
            .cardFrozenState(params.config.cardId)
            .onEach { state ->
                uiState.update(
                    TangemPayFreezeUnfreezeStateTransformer(
                        cardFrozenState = state,
                        onFreezeClick = ::onClickFreezeCard,
                        onUnfreezeClick = ::onClickUnfreezeCard,
                        converter = cardFrozenStateConverter,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    override fun onClickChangePin() {
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    override fun onClickFreezeCard() {
        uiMessageSender.send(TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard))
    }

    override fun onClickUnfreezeCard() {
        uiMessageSender.send(TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard))
    }

    private fun freezeCard() {
        modelScope.launch {
            val result = try {
                cardDetailsRepository.freezeCard(userWalletId = params.userWalletId, cardId = params.config.cardId)
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            result
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed)))
                }
                .onRight { state ->
                    when (state) {
                        TangemPayCardFrozenState.Frozen -> {
                            uiMessageSender.send(
                                SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_success)),
                            )
                            uiState.update(
                                TangemPayFreezeUnfreezeStateTransformer(
                                    cardFrozenState = state,
                                    onFreezeClick = ::onClickFreezeCard,
                                    onUnfreezeClick = ::onClickUnfreezeCard,
                                    converter = cardFrozenStateConverter,
                                ),
                            )
                        }
                        TangemPayCardFrozenState.Unfrozen -> {
                            uiMessageSender.send(
                                SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed)),
                            )
                        }
                        TangemPayCardFrozenState.Pending -> Unit // TODO [REDACTED_JIRA]
                    }
                }
        }
    }

    private fun unfreezeCard() {
        modelScope.launch {
            val result = try {
                cardDetailsRepository.unfreezeCard(userWalletId = params.userWalletId, cardId = params.config.cardId)
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            result
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed)))
                }
                .onRight { state ->
                    when (state) {
                        TangemPayCardFrozenState.Unfrozen -> {
                            uiMessageSender.send(
                                SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_success)),
                            )
                            uiState.update(
                                TangemPayFreezeUnfreezeStateTransformer(
                                    cardFrozenState = state,
                                    onFreezeClick = ::onClickFreezeCard,
                                    onUnfreezeClick = ::onClickUnfreezeCard,
                                    converter = cardFrozenStateConverter,
                                ),
                            )
                        }
                        TangemPayCardFrozenState.Frozen -> {
                            uiMessageSender.send(
                                SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed)),
                            )
                        }
                        TangemPayCardFrozenState.Pending -> Unit // TODO [REDACTED_JIRA]
                    }
                }
        }
    }

    override fun onClickAddFunds() {
        val currentBalance = balance
        val depositAddress = params.config.depositAddress
        if (currentBalance == null || depositAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = params.userWalletId,
                    fiatBalance = currentBalance.fiatBalance,
                    cryptoBalance = currentBalance.cryptoBalance,
                    depositAddress = depositAddress,
                    chainId = params.config.chainId,
                ),
            )
        }
    }

    override fun onClickWithdraw() {
        val currentBalance = balance
        val depositAddress = params.config.depositAddress
        if (currentBalance == null || depositAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
        } else {
            modelScope.launch {
                val hasActiveWithdrawal = orderRepository.hasWithdrawOrder(userWalletId = params.userWalletId)
                if (hasActiveWithdrawal) {
                    showBottomSheetError(TangemPayDetailsErrorType.WithdrawInProgress)
                } else {
                    val data = tangemPaySwapDataFactory.create(
                        depositAddress = depositAddress,
                        chainId = params.config.chainId,
                        cryptoBalance = currentBalance.cryptoBalance,
                        fiatBalance = currentBalance.fiatBalance,
                    ).getOrNull()
                    if (data != null) {
                        router.push(
                            AppRoute.Swap(
                                currencyFrom = data.currency,
                                userWalletId = data.walletId,
                                isInitialReverseOrder = false,
                                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                                tangemPayInput = AppRoute.Swap.TangemPayInput(
                                    cryptoAmount = data.cryptoBalance,
                                    fiatAmount = data.fiatBalance,
                                    depositAddress = data.depositAddress,
                                    isWithdrawal = true,
                                ),
                            ),
                        )
                    } else {
                        showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
                    }
                }
            }
        }
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = try {
                cardDetailsRepository.getCardBalance(params.userWalletId).onRight { balance = it }
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            uiState.update(DetailsBalanceTransformer(balance = result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase().onEach {
            uiState.update(DetailBalanceVisibilityTransformer(isHidden = it.isBalanceHidden))
        }.launchIn(modelScope)
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = try {
                cardDetailsRepository.isAddToWalletDone(params.userWalletId).getOrNull() == true
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = isDone,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            cardDetailsEventListener.send(CardDetailsEvent.Hide)
            txHistoryUpdateListener.triggerUpdate()
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun onClickAddToWalletBlock() {
        router.push(TangemPayDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseAddToWalletBlock() {
        modelScope.launch {
            try {
                cardDetailsRepository.setAddToWalletAsDone(params.userWalletId)
            } catch (e: Exception) {
                Timber.e(e)
            }
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = true,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onClickSwap(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                currencyFrom = data.currency,
                userWalletId = data.walletId,
                isInitialReverseOrder = true,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = data.cryptoBalance,
                    fiatAmount = data.fiatBalance,
                    depositAddress = data.depositAddress,
                    isWithdrawal = false,
                ),
            ),
        )
    }

    override fun onClickReceive(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        val config = TokenReceiveConfig(
            shouldShowWarning = false,
            cryptoCurrency = data.currency,
            userWalletId = data.walletId,
            showMemoDisclaimer = false,
            receiveAddress = data.receiveAddress,
        )
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
    }

    override fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    override fun onTransactionClick(item: TangemPayTxHistoryItem) {
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.TransactionDetails(item))
    }

    override fun onClickTermsAndLimits() {
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayMessagesFactory.createErrorMessage(type = type))
    }
}