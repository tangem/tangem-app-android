package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.hasCardWithId
import com.tangem.domain.models.account.requireCardWithId
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayReissueOrderInfo
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.ReissueCardListener
import com.tangem.features.tangempay.components.TangemPayCardPageComponent
import com.tangem.features.tangempay.components.ViewPinListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayCardPageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analytics: AnalyticsEventHandler,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
    private val reissueCardRepository: TangemPayReissueCardRepository,
) : Model(), ViewPinListener, ReissueCardListener, AddFundsListener {

    private val params: TangemPayCardPageComponent.Params = paramsContainer.require()

    private val addToWalletBannerJobHolder = JobHolder()
    private val addFundsJobHolder = JobHolder()

    val uiState: StateFlow<TangemPayCardPageUM>
        field = MutableStateFlow(
            TangemPayCardPageUM(
                onBackClick = router::pop,
                dailyLimitState = TangemPayDailyLimitBlockState.Loading,
                settings = persistentListOf(),
            ),
        )

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    // TODO v_rodionov: #[REDACTED_TASK_KEY] check reissue order state before card details are showed
    init {
        fetchAddToWalletBanner()

        paymentAccountStatusSupplier.invoke(params.userWalletId)
            .onEach { state ->
                val status = state.value
                if (status is PaymentAccountStatusValue.Loaded &&
                    status.source == StatusSource.ACTUAL &&
                    status.hasCardWithId(params.config.cardId)
                ) {
                    val card = status.requireCardWithId(params.config.cardId)
                    val limit = card.limit?.actualCardLimit?.takeIf { it.period == TangemPayCardLimitPeriod.DAY }
                    val dailyLimitState = if (limit != null) {
                        TangemPayDailyLimitBlockState.Content(
                            limit = limit.amount.format {
                                val symbol = getJavaCurrencyByCode(status.currencyCode).symbol
                                fiat(status.currencyCode, symbol)
                            },
                            onChangeClick = { router.push(TangemPayDetailsInnerRoute.LimitSetup) },
                        )
                    } else {
                        TangemPayDailyLimitBlockState.Error
                    }
                    uiState.update { it.copy(dailyLimitState = dailyLimitState, settings = buildSettings(card)) }
                } else {
                    uiState.update { it.copy(dailyLimitState = TangemPayDailyLimitBlockState.Error) }
                }
            }
            .launchIn(modelScope)
    }

    private fun buildSettings(card: TangemPayCard): ImmutableList<TangemPayCardPageSetting> {
        return persistentListOf(
            TangemPayCardPageSetting(
                title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                onSettingClick = { onClickChangePIN(card.hasPinCode) },
            ),
            TangemPayCardPageSetting(
                title = TextReference.Res(R.string.tangempay_card_details_freeze_card),
                onSettingClick = { onClickFreezeOrUnfreezeCard(card.isFrozen) },
            ),
            TangemPayCardPageSetting(
                title = TextReference.Res(R.string.tangempay_card_details_reissue_card),
                onSettingClick = ::onClickReissueCard,
            ),
        )
    }

    private fun onClickChangePIN(isPinSet: Boolean) {
        if (!isPinSet) {
            router.push(TangemPayDetailsInnerRoute.ChangePIN)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.ViewPinCode(
                    userWalletId = params.userWalletId,
                    cardId = params.config.cardId,
                ),
            )
        }
    }

    private fun onClickFreezeOrUnfreezeCard(isFrozen: Boolean) {
        val message = if (isFrozen) {
            TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard)
        } else {
            TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard)
        }
        uiMessageSender.send(message)
    }

    private fun onClickReissueCard() {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardClicked())
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.ReissueCard)
    }

    override fun onReissueOrderCreate(order: TangemPayReissueOrderInfo) {
        bottomSheetNavigation.dismiss()
        onReissueOrderStatusReceived(order.orderStatus)
        if (order.orderStatus != OrderStatus.CANCELED) {
            modelScope.launch {
                reissueCardRepository.storeReissueOrderId(params.config.cardId, order.orderId)
            }
        } else {
            uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
        }
    }

    override fun onDismissReissueCard() {
        bottomSheetNavigation.dismiss()
    }

    override fun onClickAddFunds() {
        bottomSheetNavigation.dismiss()
        modelScope.launch {
            val balance = cardDetailsRepository.getCardBalance(params.userWalletId).getOrNull()
            val depositAddress = balance?.depositAddress
            if (balance == null || depositAddress == null) {
                uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_error)))
                return@launch
            }
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = params.userWalletId,
                    fiatBalance = balance.fiatBalance,
                    cryptoBalance = balance.cryptoBalance,
                    depositAddress = depositAddress,
                    chainId = params.config.chainId,
                ),
            )
        }.saveIn(addFundsJobHolder)
    }

    override fun onClickReceive(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        val config = TokenReceiveConfig(
            shouldShowWarning = true,
            cryptoCurrency = data.currency,
            userWalletId = data.walletId,
            showMemoDisclaimer = false,
            receiveAddress = data.receiveAddress,
        )
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
    }

    override fun onClickSwap(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                cryptoCurrency = data.currency,
                userWalletId = data.walletId,
                currencyPosition = AppRoute.Swap.CurrencyPosition.TO,
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

    override fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    private fun freezeCard() {
        modelScope.launch {
            cardDetailsRepository.freezeCard(
                userWalletId = params.userWalletId,
                cardId = params.config.cardId,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed))
                uiMessageSender.send(message)
            }.onRight { state ->
                val message = if (state == TangemPayCardFrozenState.Frozen) {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_success))
                } else {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed))
                }
                uiMessageSender.send(message)
            }
        }
    }

    private fun unfreezeCard() {
        modelScope.launch {
            cardDetailsRepository.unfreezeCard(
                userWalletId = params.userWalletId,
                cardId = params.config.cardId,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed))
                uiMessageSender.send(message)
            }.onRight { state ->
                val message = if (state == TangemPayCardFrozenState.Unfrozen) {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_success))
                } else {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed))
                }
                uiMessageSender.send(message)
            }
        }
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = cardDetailsRepository.isAddToWalletDone(params.userWalletId).getOrNull() == true
            if (!isDone) {
                uiState.update { state ->
                    state.copy(
                        addToWalletBlockState = AddToWalletBlockState(
                            onClick = ::onClickAddToWallet,
                            onClickClose = ::onClickCloseBanner,
                        ),
                    )
                }
            }
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun onClickAddToWallet() {
        router.push(TangemPayDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseBanner() {
        modelScope.launch {
            cardDetailsRepository.setAddToWalletAsDone(params.userWalletId)
            uiState.update { it.copy(addToWalletBlockState = null) }
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onClickChangePin() {
        bottomSheetNavigation.dismiss()
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    override fun onDismissViewPin() {
        bottomSheetNavigation.dismiss()
    }

    private fun onReissueOrderStatusReceived(orderStatus: OrderStatus) {
        when (orderStatus) {
            OrderStatus.NEW, OrderStatus.PROCESSING, OrderStatus.COMPLETED, OrderStatus.UNKNOWN -> {
                uiState.update { state ->
                    state.copy(
                        addToWalletBlockState = null,
                        isReissueInProgress = true,
                    )
                }
            }
            OrderStatus.CANCELED -> Unit
        }
    }
}