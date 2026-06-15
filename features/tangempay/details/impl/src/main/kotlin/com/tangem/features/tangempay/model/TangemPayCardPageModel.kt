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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.findCardWithId
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isFrozen
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.usecase.ChangeCardFrozenStateUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.closure.CloseCardListener
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.ReissueCardListener
import com.tangem.features.tangempay.components.TangemPayCardPageComponent
import com.tangem.features.tangempay.components.ViewPinListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.model.controller.TangemPayCardDetailsController
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.navigation.TangemPayCardDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.features.tangempay.utils.cryptoCurrency
import com.tangem.features.tangempay.utils.ifLoadedOrNull
import com.tangem.features.tangempay.utils.userWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analytics: AnalyticsEventHandler,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
    private val changeCardFrozenStateUseCase: ChangeCardFrozenStateUseCase,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val cardDetailsControllerFactory: TangemPayCardDetailsController.Factory,
) : Model(), ViewPinListener, ReissueCardListener, AddFundsListener, CloseCardListener {

    private val params: TangemPayCardPageComponent.Params = paramsContainer.require()

    private val addToWalletBannerJobHolder = JobHolder()
    private val addFundsJobHolder = JobHolder()
    private val frozenStateJobHolder = JobHolder()
    private val reloadLimitsJobHolder = JobHolder()

    private val currentStatus = MutableStateFlow(params.initialStatus)
    private val userWalletId = currentStatus.value.userWalletId

    /**
     * Currently focused card in the swipe pager. Per-card actions and the "Details" reveal target it.
     * Initialized to the card the user tapped on the account screen ([Params.cardId]); falls back to
     * the first available card if it is gone (handled in [syncCardControllers]).
     */
    private val selectedCardId = MutableStateFlow(params.cardId)
    val selectedCardIdState: StateFlow<String> = selectedCardId

    private val cardControllers = linkedMapOf<String, TangemPayCardDetailsController>()
    private val _cardControllersState: MutableStateFlow<ImmutableList<TangemPayCardDetailsController>> =
        MutableStateFlow(persistentListOf())
    val cardControllersState: StateFlow<ImmutableList<TangemPayCardDetailsController>> = _cardControllersState

    private val cryptoCurrency
        get() = currentStatus.value.cryptoCurrency

    // Multiple cards are temporarily gated behind the same toggle as close-card.
    private val isMultipleCardsEnabled: Boolean get() = tangemPayFeatureToggles.isCloseCardEnabled

    val uiState: StateFlow<TangemPayCardPageUM>
        field = MutableStateFlow(
            TangemPayCardPageUM(
                onBackClick = router::pop,
                dailyLimitState = TangemPayDailyLimitBlockState.Loading,
                settings = persistentListOf(),
                settingsV2 = persistentListOf(),
                menuItems = buildMenuItems(
                    isLastCard = params.initialStatus.ifLoadedOrNull { it.cards.isLastCard() } ?: false,
                ),
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
                syncCardControllers(state)
            }
            .launchIn(modelScope)

        combine(currentStatus, selectedCardId) { status, selectedId -> status to selectedId }
            .onEach { (status, selectedId) -> updateSelectedCardUi(status, selectedId) }
            .launchIn(modelScope)
    }

    override fun onDestroy() {
        cardDetailsEventListener.send(CardDetailsEvent.HideAll)
        super.onDestroy()
    }

    private fun buildDailyLimitState(state: AccountStatus.Payment): TangemPayDailyLimitBlockState {
        val status = state.value
        val card = if (status is PaymentAccountStatusValue.Loaded && status.source == StatusSource.ACTUAL) {
            status.findCardWithId(selectedCardId.value)
        } else {
            null
        }
        val limit = card?.limit?.actualCardLimit?.takeIf { it.period == TangemPayCardLimitPeriod.DAY }
        return if (status is PaymentAccountStatusValue.Loaded && limit != null) {
            TangemPayDailyLimitBlockState.Content(
                limit = limit.amount.format {
                    val currencyCode = status.balance.fiatBalance.currency
                    val symbol = getJavaCurrencyByCode(currencyCode).symbol
                    fiat(currencyCode, symbol).optionalDecimals()
                },
                onChangeClick = ::onClickLimitChange,
            )
        } else {
            TangemPayDailyLimitBlockState.Error(onReloadClick = ::onClickReloadLimits)
        }
    }

    fun isRedesignEnabled(): Boolean = tangemPayFeatureToggles.isRedesignEnabled

    /** Reports the card the user swiped to so per-card UI and reveal target it. */
    fun onCardPageSelected(index: Int) {
        cardControllersState.value.getOrNull(index)?.let { selectedCardId.value = it.cardId }
    }

    private fun childCardScope(): CoroutineScope =
        CoroutineScope(modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext[Job]))

    private fun syncCardControllers(state: AccountStatus.Payment) {
        val status = state.value
        if (status !is PaymentAccountStatusValue.Loaded || status.source != StatusSource.ACTUAL) return

        val cards = status.cards.let { if (isMultipleCardsEnabled) it else it.take(1) }
        val newIds = cards.mapTo(mutableSetOf()) { it.id }

        cardControllers.keys.filterNot { it in newIds }.toList().forEach { removedId ->
            cardControllers.remove(removedId)?.let { controller ->
                controller.dispose()
                cardDetailsEventListener.send(CardDetailsEvent.Hide(removedId))
            }
        }

        val ordered = LinkedHashMap<String, TangemPayCardDetailsController>(cards.size)
        cards.forEach { card ->
            ordered[card.id] = cardControllers[card.id] ?: cardDetailsControllerFactory.create(
                scope = childCardScope(),
                initialCard = card,
                userWalletId = userWalletId,
                config = TangemPayCardDetailsController.Config(
                    isEditingNameEnabled = true,
                    shouldShowCardDetailsButtonOnCard = false,
                ),
                onEditNameClick = { router.push(TangemPayCardDetailsInnerRoute.EditCardDisplayName(card.id)) },
            )
        }
        cardControllers.clear()
        cardControllers.putAll(ordered)
        _cardControllersState.value = ordered.values.toList().toImmutableList()

        if (selectedCardId.value !in newIds) {
            ordered.keys.firstOrNull()?.let { selectedCardId.value = it }
        }
    }

    private fun updateSelectedCardUi(state: AccountStatus.Payment, selectedId: String) {
        val status = state.value
        if (status is PaymentAccountStatusValue.Loaded && status.source == StatusSource.ACTUAL) {
            val card = status.findCardWithId(selectedId) ?: return
            uiState.update { uiState ->
                uiState.copy(
                    dailyLimitState = buildDailyLimitState(state),
                    settings = buildSettings(card),
                    settingsV2 = buildSettingsV2(card),
                    menuItems = buildMenuItems(isLastCard = status.cards.isLastCard()),
                    cardState = card.state,
                )
            }
        } else {
            uiState.update { it.copy(dailyLimitState = buildDailyLimitState(state)) }
        }
    }

    private fun selectedCard(): TangemPayCard? {
        val status = currentStatus.value.value
        return (status as? PaymentAccountStatusValue.Loaded)?.findCardWithId(selectedCardId.value)
    }

    private fun buildSettings(card: TangemPayCard): ImmutableList<TangemPayCardPageSetting> {
        if (isRedesignEnabled()) return persistentListOf()
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
        if (!isRedesignEnabled()) return
        combine(cardDetailsEventListener.event, selectedCardId) { event, selectedId ->
            event is CardDetailsEvent.Show && event.cardId == selectedId
        }.collect { isDetailsShown ->
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
        if (!isRedesignEnabled()) return persistentListOf()
        return persistentListOf(
            TangemPayCardPageSettingV2(
                id = TangemPayCardPageSettingV2.Id.Details,
                title = TextReference.Res(R.string.tangempay_card_details_title),
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

    private fun buildMenuItems(isLastCard: Boolean): ImmutableList<TangemPayDropDownItemUM> {
        return buildList {
            add(
                TangemPayDropDownItemUM(
                    title = TextReference.Res(R.string.tangempay_card_details_reissue_card),
                    onClick = ::onClickReissueCard,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_arrow_refresh_20,
                        tintReference = {
                            TangemTheme.colors3.icon.primary
                        },
                    ),
                ),
            )
            if (tangemPayFeatureToggles.isCloseCardEnabled) {
                add(
                    TangemPayDropDownItemUM(
                        title = TextReference.Res(R.string.tangem_pay_close_card_popup_primary_button_title),
                        onClick = ::onClickCloseCard,
                        icon = TangemIconUM.Icon(
                            iconRes = CoreUiR.drawable.ic_trash_24,
                            tintReference = {
                                if (isLastCard) {
                                    TangemTheme.colors3.icon.tertiary
                                } else {
                                    TangemTheme.colors3.icon.primary
                                }
                            },
                        ),
                        subtitle = if (isLastCard) {
                            TextReference.Res(R.string.tangem_pay_close_card_disabled_last_card)
                        } else {
                            null
                        },
                        isEnabled = !isLastCard,
                    ),
                )
            }
        }.toImmutableList()
    }

    private fun onClickViewDetails() {
        cardDetailsEventListener.send(CardDetailsEvent.Show(selectedCardId.value))
    }

    private fun onClickReloadLimits() {
        if (reloadLimitsJobHolder.isActive) return
        uiState.update { it.copy(dailyLimitState = TangemPayDailyLimitBlockState.Loading) }
        modelScope.launch {
            paymentAccountStatusFetcher(userWalletId)
            uiState.update { it.copy(dailyLimitState = buildDailyLimitState(currentStatus.value)) }
        }.saveIn(reloadLimitsJobHolder)
    }

    private fun onClickLimitChange() {
        analytics.send(TangemPayAnalyticsEvents.LimitChangeClicked())
        router.push(TangemPayCardDetailsInnerRoute.LimitSetup)
    }

    private fun onClickChangePIN(isPinSet: Boolean) {
        if (!isPinSet) {
            router.push(TangemPayCardDetailsInnerRoute.ChangePIN)
        } else {
            val card = selectedCard() ?: return
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
            TangemPayMessagesFactory.createUnfreezeCardMessage(
                onUnfreezeClicked = ::unfreezeCard,
                isRedesignEnabled = tangemPayFeatureToggles.isRedesignEnabled,
            )
        } else {
            TangemPayMessagesFactory.createFreezeCardMessage(
                onFreezeClicked = ::freezeCard,
                isRedesignEnabled = tangemPayFeatureToggles.isRedesignEnabled,
            )
        }
        uiMessageSender.send(message)
    }

    private fun onClickReissueCard() {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardClicked())
        bottomSheetNavigation.activate(TangemPayCardNavigation.ReissueCard(cardId = selectedCardId.value))
    }

    override fun onDismissReissueCard() {
        bottomSheetNavigation.dismiss()
    }

    override fun onDismissCloseCard() {
        bottomSheetNavigation.dismiss()
    }

    private fun onClickCloseCard() {
        analytics.send(TangemPayAnalyticsEvents.CloseCardClicked())
        val card = selectedCard() ?: return
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.CloseCard(
                userWalletId = userWalletId,
                cardId = card.id,
            ),
        )
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
        val card = selectedCard() ?: return
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
        val card = selectedCard() ?: return
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

private fun List<TangemPayCard>.isLastCard(): Boolean = count { it.state == TangemPayCardState.Active } <= 1