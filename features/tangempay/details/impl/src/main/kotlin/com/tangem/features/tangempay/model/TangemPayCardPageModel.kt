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
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isFrozen
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.usecase.ChangeCardFrozenStateUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.ReissueCardListener
import com.tangem.features.tangempay.components.TangemPayCardPageComponent
import com.tangem.features.tangempay.components.ViewPinListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.navigation.TangemPayCardDetailsInnerRoute
import com.tangem.features.tangempay.utils.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList", "LargeClass")
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
    private val changeCardFrozenStateUseCase: ChangeCardFrozenStateUseCase,
    private val designFeatureToggles: DesignFeatureToggles,
    private val cardDetailsEventListener: CardDetailsEventListener,
) : Model(), ViewPinListener, ReissueCardListener, AddFundsListener {

    private val params: TangemPayCardPageComponent.Params = paramsContainer.require()

    private val addToWalletBannerJobHolder = JobHolder()
    private val addFundsJobHolder = JobHolder()
    private val frozenStateJobHolder = JobHolder()

    private val currentStatus = MutableStateFlow(params.initialStatus)
    private val initialCardId = params.initialStatus.firstCard().id
    private val userWalletId = currentStatus.value.userWalletId

    private val cryptoCurrency
        get() = currentStatus.value.cryptoCurrency

    val uiState: StateFlow<TangemPayCardPageUM>
        field = MutableStateFlow(
            TangemPayCardPageUM(
                onBackClick = router::pop,
                dailyLimitState = TangemPayDailyLimitBlockState.Loading,
                settings = persistentListOf(),
                settingsV2 = persistentListOf(),
                menuItems = buildMenuItems(),
            ),
        )

    val bottomSheetNavigation: SlotNavigation<TangemPayCardNavigation> = SlotNavigation()

    init {
        analytics.send(TangemPayAnalyticsEvents.CardManagementScreenOpened())
        fetchAddToWalletBanner()
        modelScope.launch { subscribeOnDetailsState() }

        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { state ->
                currentStatus.update { state }
                val status = state.value
                if (status is PaymentAccountStatusValue.Loaded && status.source == StatusSource.ACTUAL) {
                    val card = state.findCard(initialCardId, params.initialStatus) ?: return@onEach
                    val limit = card.limit?.actualCardLimit?.takeIf { it.period == TangemPayCardLimitPeriod.DAY }
                    val dailyLimitState = if (limit != null) {
                        TangemPayDailyLimitBlockState.Content(
                            limit = limit.amount.format {
                                val symbol = getJavaCurrencyByCode(status.currencyCode).symbol
                                fiat(status.currencyCode, symbol).optionalDecimals()
                            },
                            onChangeClick = ::onClickLimitChange,
                        )
                    } else {
                        TangemPayDailyLimitBlockState.Error
                    }
                    uiState.update { uiState ->
                        uiState.copy(
                            dailyLimitState = dailyLimitState,
                            settings = buildSettings(card),
                            settingsV2 = buildSettingsV2(card),
                            isReissueInProgress = card.state == TangemPayCardState.Reissuing,
                        )
                    }
                } else {
                    uiState.update { it.copy(dailyLimitState = TangemPayDailyLimitBlockState.Error) }
                }
            }
            .launchIn(modelScope)
    }

    private fun buildSettings(card: TangemPayCard): ImmutableList<TangemPayCardPageSetting> {
        if (designFeatureToggles.isRedesignEnabled) return persistentListOf()
        return persistentListOf(
            TangemPayCardPageSetting(
                title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                onSettingClick = { onClickChangePIN(card.hasPinCode) },
                testTag = TangemPayTestTags.CHANGE_PIN_ROW,
            ),
            TangemPayCardPageSetting(
                title = TextReference.Res(
                    if (card.isFrozen) {
                        R.string.tangempay_card_details_unfreeze_card
                    } else {
                        R.string.tangempay_card_details_freeze_card
                    },
                ),
                onSettingClick = { onClickFreezeOrUnfreezeCard(card.isFrozen) },
                testTag = TangemPayTestTags.FREEZE_CARD_ROW,
            ),
            TangemPayCardPageSetting(
                title = TextReference.Res(R.string.tangempay_card_details_reissue_card),
                onSettingClick = ::onClickReissueCard,
            ),
        )
    }

    private suspend fun subscribeOnDetailsState() {
        if (!designFeatureToggles.isRedesignEnabled) return
        cardDetailsEventListener.event.collect { event ->
            val isDetailsShown = event == CardDetailsEvent.Show
            uiState.update { state ->
                state.copy(
                    settingsV2 = state.settingsV2
                        .map { setting ->
                            if (setting.id == TangemPayCardPageSettingV2.Id.Details) {
                                setting.copy(isEnabled = !isDetailsShown)
                            } else {
                                setting
                            }
                        }
                        .toImmutableList(),
                )
            }
        }
    }

    private fun buildSettingsV2(card: TangemPayCard): ImmutableList<TangemPayCardPageSettingV2> {
        if (!designFeatureToggles.isRedesignEnabled) return persistentListOf()
        return persistentListOf(
            TangemPayCardPageSettingV2(
                id = TangemPayCardPageSettingV2.Id.Details,
                title = TextReference.Res(R.string.details_title),
                onClick = ::onClickViewDetails,
                iconRes = CoreUiR.drawable.ic_visa_card_details_24,
            ),
            TangemPayCardPageSettingV2(
                id = TangemPayCardPageSettingV2.Id.Freeze,
                title = TextReference.Res(
                    if (card.isFrozen) {
                        R.string.tangem_pay_freeze_card_unfreeze
                    } else {
                        R.string.tangem_pay_freeze_card_freeze
                    },
                ),
                onClick = { onClickFreezeOrUnfreezeCard(card.isFrozen) },
                iconRes = CoreUiR.drawable.ic_freeze_24,
                testTag = TangemPayTestTags.FREEZE_CARD_ROW,
            ),
            TangemPayCardPageSettingV2(
                id = TangemPayCardPageSettingV2.Id.ChangePin,
                title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                onClick = { onClickChangePIN(card.hasPinCode) },
                iconRes = CoreUiR.drawable.ic_card_pin_24,
                testTag = TangemPayTestTags.CHANGE_PIN_ROW,
            ),
        )
    }

    private fun buildMenuItems(): ImmutableList<TangemPayDropDownItemUM> {
        return persistentListOf(
            TangemPayDropDownItemUM(
                title = TextReference.Res(R.string.tangempay_card_details_reissue_card),
                onClick = ::onClickReissueCard,
                icon = TangemIconUM.Icon(
                    iconRes = CoreUiR.drawable.ic_replace_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
            ),
        )
    }

    private fun onClickViewDetails() {
        modelScope.launch(dispatchers.default) {
            cardDetailsEventListener.send(CardDetailsEvent.Show)
        }
    }

    private fun onClickLimitChange() {
        analytics.send(TangemPayAnalyticsEvents.LimitChangeClicked())
        router.push(TangemPayCardDetailsInnerRoute.LimitSetup)
    }

    private fun onClickChangePIN(isPinSet: Boolean) {
        if (!isPinSet) {
            router.push(TangemPayCardDetailsInnerRoute.ChangePIN)
        } else {
            val card = currentStatus.value.findCard(initialCardId, params.initialStatus) ?: return
            bottomSheetNavigation.activate(
                TangemPayCardNavigation.ViewPinCode(
                    userWalletId = userWalletId,
                    cardId = card.id,
                ),
            )
        }
    }

    private fun onClickFreezeOrUnfreezeCard(isFrozen: Boolean) {
        if (frozenStateJobHolder.isActive) return

        val message = if (isFrozen) {
            TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard)
        } else {
            TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard)
        }
        uiMessageSender.send(message)
    }

    private fun onClickReissueCard() {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardClicked())
        bottomSheetNavigation.activate(TangemPayCardNavigation.ReissueCard)
    }

    override fun onDismissReissueCard() {
        bottomSheetNavigation.dismiss()
    }

    override fun onClickAddFunds() {
        bottomSheetNavigation.dismiss()
        modelScope.launch {
            val balance = cardDetailsRepository.getCardBalance(userWalletId).getOrNull()
            val depositAddress = balance?.depositAddress
            if (balance == null || depositAddress == null) {
                uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_error)))
                return@launch
            }
            bottomSheetNavigation.activate(
                TangemPayCardNavigation.AddFunds(
                    walletId = userWalletId,
                    fiatBalance = balance.fiatBalance,
                    cryptoBalance = balance.cryptoBalance,
                    depositAddress = depositAddress,
                    cryptoCurrency = cryptoCurrency,
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
        bottomSheetNavigation.activate(TangemPayCardNavigation.Receive(config))
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
        val card = currentStatus.value.findCard(initialCardId, params.initialStatus) ?: return
        modelScope.launch {
            changeCardFrozenStateUseCase(
                userWalletId = userWalletId,
                cardId = card.id,
                isFreezing = true,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed))
                uiMessageSender.send(message)
            }.onRight {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_success))
                uiMessageSender.send(message)
            }
        }.saveIn(frozenStateJobHolder)
    }

    private fun unfreezeCard() {
        val card = currentStatus.value.findCard(initialCardId, params.initialStatus) ?: return
        modelScope.launch {
            changeCardFrozenStateUseCase(
                userWalletId = userWalletId,
                cardId = card.id,
                isFreezing = false,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed))
                uiMessageSender.send(message)
            }.onRight {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_success))
                uiMessageSender.send(message)
            }
        }.saveIn(frozenStateJobHolder)
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = cardDetailsRepository.isAddToWalletDone(userWalletId).getOrNull() == true
            if (!isDone) {
                uiState.update { state ->
                    state.copy(
                        addToWalletBlockState = AddToWalletBlockState(
                            onClick = ::onClickAddToWallet,
                            onClickClose = ::onClickCloseBanner,
                            shouldUseMagicEffect = false,
                        ),
                    )
                }
            }
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun onClickAddToWallet() {
        router.push(TangemPayCardDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseBanner() {
        modelScope.launch {
            cardDetailsRepository.setAddToWalletAsDone(userWalletId)
            uiState.update { it.copy(addToWalletBlockState = null) }
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onClickChangePin() {
        bottomSheetNavigation.dismiss()
        router.push(TangemPayCardDetailsInnerRoute.ChangePIN)
    }

    override fun onDismissViewPin() {
        bottomSheetNavigation.dismiss()
    }
}