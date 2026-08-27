package com.tangem.features.tangempay.card.details

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.biometric.BiometricAuthError
import com.tangem.core.biometric.BiometricAuthManager
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
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
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.account.findCardWithId
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isFrozen
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.usecase.ChangeCardFrozenStateUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.addfunds.AddFundsListener
import com.tangem.features.tangempay.card.closure.CloseCardListener
import com.tangem.features.tangempay.card.gpay.AddToWalletBlockState
import com.tangem.features.tangempay.card.pin.ViewPinListener
import com.tangem.features.tangempay.card.reissue.ReissueCardListener
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.common.balanceOrNull
import com.tangem.features.tangempay.common.ifLoadedOrNull
import com.tangem.features.tangempay.common.userWalletId
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.multichain.choosenetwork.ChooseNetworkListener
import com.tangem.features.tangempay.multichain.shouldUseChooseNetwork
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
@Stable
@ModelScoped
internal class TangemPayCardPageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analytics: AnalyticsEventHandler,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
    private val changeCardFrozenStateUseCase: ChangeCardFrozenStateUseCase,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val cardDetailsControllerFactory: TangemPayCardDetailsController.Factory,
    private val biometricAuthManager: BiometricAuthManager,
    private val settingsManager: SettingsManager,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
) : Model(), ViewPinListener, ReissueCardListener, AddFundsListener, CloseCardListener, ChooseNetworkListener {

    private val params: TangemPayCardPageComponent.Params = paramsContainer.require()

    private val addFundsJobHolder = JobHolder()
    private val changeFrozenStateJobHolder = JobHolder()
    private val reloadLimitsJobHolder = JobHolder()
    private val viewPinAuthJobHolder = JobHolder()
    private val frozenStateJobHolder = JobHolder()

    private val currentStatus = MutableStateFlow(params.initialStatus)
    private val userWalletId = currentStatus.value.userWalletId

    val selectedCardId: StateFlow<String>
        field = MutableStateFlow(params.cardId)

    private val cardControllers = linkedMapOf<String, TangemPayCardDetailsController>()

    val cardControllersState: StateFlow<ImmutableList<TangemPayCardDetailsController>>
        field = MutableStateFlow(persistentListOf())

    private val cryptoCurrency = tangemPayCurrencyFactory.create(userWalletId)

    private val shouldShowGooglePayBanner = MutableStateFlow(false)

    val uiState: StateFlow<TangemPayCardPageUM>
        field = MutableStateFlow(
            TangemPayCardPageUM(
                onBackClick = router::pop,
                dailyLimitState = TangemPayDailyLimitBlockState.Loading,
                settings = persistentListOf(),
                menuItems = buildMenuItems(
                    isLastCard = params.initialStatus.ifLoadedOrNull { it.cards.isLastCard() } ?: false,
                ),
            ),
        )

    val bottomSheetNavigation: SlotNavigation<TangemPayCardNavigation> = SlotNavigation()

    init {
        analytics.send(TangemPayAnalyticsEvents.CardManagementScreenOpened())

        modelScope.launch {
            shouldShowGooglePayBanner.update {
                cardDetailsRepository.isAddToWalletDone(userWalletId).getOrNull() == false
            }
        }

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
        val fiatCurrencyCode = (status as? PaymentAccountStatusValue.Loaded)?.balance?.fiatBalance?.currency
        return if (fiatCurrencyCode != null && limit != null) {
            TangemPayDailyLimitBlockState.Content(
                limit = limit.amount.format {
                    val symbol = getJavaCurrencyByCode(fiatCurrencyCode).symbol
                    fiat(fiatCurrencyCode, symbol).optionalDecimals()
                },
                onChangeClick = ::onClickLimitChange,
            )
        } else {
            TangemPayDailyLimitBlockState.Error(onReloadClick = ::onClickReloadLimits)
        }
    }

    /** Reports the card the user swiped to so per-card UI and reveal target it. */
    fun onCardPageSelected(index: Int) {
        cardControllersState.value.getOrNull(index)?.let { selectedCardId.value = it.cardId }
    }

    private fun childCardScope(): CoroutineScope =
        CoroutineScope(modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext[Job]))

    private fun syncCardControllers(state: AccountStatus.Payment) {
        val status = state.value
        if (status !is PaymentAccountStatusValue.Loaded || status.source != StatusSource.ACTUAL) return

        val cards = status.cards
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
                card = card,
                userWalletId = userWalletId,
                config = TangemPayCardDetailsController.Config(
                    isEditingNameEnabled = true,
                    shouldShowCardDetailsButtonOnCard = false,
                ),
                onEditNameClick = { router.push(TangemPayCardDetailsInnerRoute.EditCardDisplayName(card)) },
            )
        }
        cardControllers.clear()
        cardControllers.putAll(ordered)
        cardControllersState.update { ordered.values.toPersistentList() }

        if (selectedCardId.value !in newIds) {
            ordered.keys.firstOrNull()?.let { selectedCardId.value = it }
        }
    }

    private fun updateSelectedCardUi(state: AccountStatus.Payment, selectedId: String) {
        val status = state.value
        if (status is PaymentAccountStatusValue.Loaded && status.source == StatusSource.ACTUAL) {
            val card = status.findCardWithId(selectedId) ?: return
            updateGooglePayBannerState(card.frozenState)
            uiState.update { uiState ->
                uiState.copy(
                    dailyLimitState = buildDailyLimitState(state),
                    settings = status.buildSettings(card.frozenState),
                    menuItems = buildMenuItems(isLastCard = status.cards.isLastCard()),
                    cardState = card.state,
                )
            }
        } else {
            uiState.update { it.copy(dailyLimitState = buildDailyLimitState(state)) }
        }

        subscribeToCardFrozenState(selectedId)
    }

    private fun selectedCard(): TangemPayCard? {
        val status = currentStatus.value.value
        return (status as? PaymentAccountStatusValue.Loaded)?.findCardWithId(selectedCardId.value)
    }

    private suspend fun subscribeOnDetailsState() {
        combine(cardDetailsEventListener.event, selectedCardId) { event, selectedId ->
            event is CardDetailsEvent.Show && event.cardId == selectedId
        }.collect { isDetailsShown ->
            uiState.update { state ->
                state.copy(
                    settings = state.settings
                        .map { setting ->
                            if (setting.id == TangemPayCardPageSetting.Id.Details) {
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

    private fun subscribeToCardFrozenState(cardId: String) {
        cardDetailsRepository.cardFrozenState(cardId)
            .onEach { cardFrozenState ->
                updateGooglePayBannerState(cardFrozenState)
                uiState.update { state ->
                    val settings = currentStatus.value.ifLoadedOrNull { it.buildSettings(cardFrozenState) }
                    state.copy(settings = settings ?: persistentListOf())
                }
            }
            .launchIn(modelScope)
            .saveIn(frozenStateJobHolder)
    }

    private fun updateGooglePayBannerState(cardFrozenState: TangemPayCardFrozenState) {
        uiState.update { state ->
            state.copy(
                addToWalletBlockState = AddToWalletBlockState(
                    onClick = ::onClickAddToWallet,
                    onClickClose = ::onClickCloseBanner,
                ).takeIf {
                    shouldShowGooglePayBanner.value && cardFrozenState == TangemPayCardFrozenState.Unfrozen
                },
            )
        }
    }

    private fun PaymentAccountStatusValue.Loaded.buildSettings(
        frozenState: TangemPayCardFrozenState,
    ): ImmutableList<TangemPayCardPageSetting> {
        val card = findCardWithId(selectedCardId.value) ?: return persistentListOf()
        return persistentListOf(
            TangemPayCardPageSetting(
                id = TangemPayCardPageSetting.Id.Details,
                title = TextReference.Res(R.string.tangempay_card_details_title),
                onClick = ::onClickViewDetails,
                iconRes = CoreUiR.drawable.ic_visa_card_details_24,
                testTag = TangemPayTestTags.SHOW_DETAILS_ROW,
                isEnabled = frozenState == TangemPayCardFrozenState.Unfrozen,
            ),
            TangemPayCardPageSetting(
                id = TangemPayCardPageSetting.Id.Freeze,
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
                isLoading = frozenState == TangemPayCardFrozenState.Pending,
            ),
            TangemPayCardPageSetting(
                id = TangemPayCardPageSetting.Id.ChangePin,
                title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                onClick = { onClickChangePIN(card.hasPinCode) },
                iconRes = CoreUiR.drawable.ic_card_pin_24,
                testTag = TangemPayTestTags.CHANGE_PIN_ROW,
                isEnabled = frozenState == TangemPayCardFrozenState.Unfrozen,
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
        val card = selectedCard() ?: return
        analytics.send(TangemPayAnalyticsEvents.LimitChangeClicked())
        router.push(TangemPayCardDetailsInnerRoute.LimitSetup(card))
    }

    private fun onClickChangePIN(isPinSet: Boolean) {
        val card = selectedCard() ?: return
        when {
            !isPinSet -> router.push(TangemPayCardDetailsInnerRoute.ChangePIN(card))
            tangemPayFeatureToggles.isPinBiometryGateEnabled -> {
                if (viewPinAuthJobHolder.isActive) return
                modelScope.launch {
                    val result = biometricAuthManager.authenticate(
                        BiometricAuthManager.Config(
                            title = resourceReference(R.string.tangempay_card_details_view_pin_code_title),
                            subtitle = null,
                        ),
                    )
                    when (result) {
                        is BiometricAuthManager.Result.Success -> showPinCode(cardId = card.id)
                        is BiometricAuthManager.Result.Cancelled -> Unit
                        is BiometricAuthManager.Result.Failure -> onViewPinAuthFailure(result)
                    }
                }.saveIn(viewPinAuthJobHolder)
            }
            else -> showPinCode(cardId = card.id)
        }
    }

    private fun showPinCode(cardId: String) {
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.ViewPinCode(
                userWalletId = userWalletId,
                cardId = cardId,
            ),
        )
    }

    private fun onViewPinAuthFailure(failure: BiometricAuthManager.Result.Failure) {
        when (failure.error) {
            BiometricAuthError.NoDeviceCredential -> {
                uiMessageSender.send(
                    message = TangemPayMessagesFactory.createProtectionNotSetMessage(
                        onOpenSettingsClick = settingsManager::openScreenLockSettings,
                    ),
                )
            }
            BiometricAuthError.NoBiometricEnrolled,
            BiometricAuthError.HardwareUnavailable,
            BiometricAuthError.NoForegroundActivity,
            BiometricAuthError.Unknown,
            -> {
                uiMessageSender.send(
                    message = SnackbarMessage(resourceReference(CoreUiR.string.common_unknown_error)),
                )
            }
            BiometricAuthError.LockedOutPermanently -> {
                uiMessageSender.send(
                    message = SnackbarMessage(
                        resourceReference(CoreUiR.string.biometric_lockout_permanent_warning_title),
                    ),
                )
            }
            BiometricAuthError.SecurityUpdateRequired -> {
                uiMessageSender.send(
                    message = SnackbarMessage(
                        resourceReference(CoreUiR.string.alert_authentication_error_message),
                    ),
                )
            }
            BiometricAuthError.LockedOut,
            BiometricAuthError.SystemCancelled,
            BiometricAuthError.Timeout,
            -> Unit
        }
    }

    private fun onClickFreezeOrUnfreezeCard(isFrozen: Boolean) {
        if (changeFrozenStateJobHolder.isActive) return

        val message = if (isFrozen) {
            TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard)
        } else {
            TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard)
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
            val balance = currentStatus.value.balanceOrNull()
            if (balance == null) {
                val message = TangemPayMessagesFactory.createErrorMessage(TangemPayDetailsErrorType.Receive)
                uiMessageSender.send(message)
            } else {
                bottomSheetNavigation.activate(
                    TangemPayCardNavigation.AddFunds(
                        walletId = userWalletId,
                        fiatBalance = balance.fiatBalance.availableBalance,
                        cryptoBalance = balance.cryptoBalance.balance,
                        depositAddress = balance.cryptoBalance.depositAddress,
                        cryptoCurrency = cryptoCurrency,
                        virtualAccountOnramp = currentStatus.value.ifLoadedOrNull { it.virtualAccount },
                    ),
                )
            }
        }.saveIn(addFundsJobHolder)
    }

    override fun onClickReceive(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        val loaded = currentStatus.value.ifLoadedOrNull { it }
        val shouldChooseNetwork = loaded != null &&
            shouldUseChooseNetwork(tangemPayFeatureToggles.isAccountMultichainEnabled, loaded.networks)
        if (shouldChooseNetwork) {
            bottomSheetNavigation.activate(TangemPayCardNavigation.ChooseNetwork(walletId = data.walletId))
        } else {
            val config = TokenReceiveConfig(
                shouldShowWarning = true,
                cryptoCurrency = data.currency,
                userWalletId = data.walletId,
                showMemoDisclaimer = false,
                receiveAddress = data.receiveAddress,
            )
            bottomSheetNavigation.activate(TangemPayCardNavigation.Receive(config))
        }
    }

    override fun onClickSwap(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                fromCryptoCurrency = data.currency,
                userWalletId = data.walletId,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = data.cryptoBalance,
                    fiatAmount = data.fiatBalance,
                    depositAddress = data.depositAddress,
                ),
            ),
        )
    }

    override fun onClickBankTransfer() {
        val loaded = currentStatus.value.ifLoadedOrNull { it } ?: return
        when (val onramp = loaded.virtualAccount) {
            null -> return
            VirtualAccountOnramp.Processing -> showVaPreparing()
            is VirtualAccountOnramp.Available,
            VirtualAccountOnramp.Eligible,
            -> openVirtualAccountDeposit(onramp, loaded)
        }
    }

    private fun openVirtualAccountDeposit(onramp: VirtualAccountOnramp, loaded: PaymentAccountStatusValue.Loaded) {
        analytics.send(TangemPayAnalyticsEvents.VaTopupButtonClicked())
        bottomSheetNavigation.dismiss()
        val paymentAccountAddress = loaded.balance?.cryptoBalance?.depositAddress
        if (paymentAccountAddress == null) {
            uiMessageSender.send(TangemPayMessagesFactory.createErrorMessage(TangemPayDetailsErrorType.Receive))
            return
        }
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.VirtualAccountDeposit(
                virtualAccountOnramp = onramp,
                userWalletId = userWalletId,
                paymentAccountAddress = paymentAccountAddress,
            ),
        )
    }

    fun showVaBankingDetailsError(productInstanceId: String) {
        analytics.send(TangemPayAnalyticsEvents.VaDetailsErrorShowed())
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.VaBankingDetailsError(
                userWalletId = userWalletId,
                productInstanceId = productInstanceId,
            ),
        )
    }

    private fun showVaPreparing() {
        analytics.send(TangemPayAnalyticsEvents.VaPreparationPopupShowed())
        bottomSheetNavigation.dismiss()
        uiMessageSender.send(message = TangemPayMessagesFactory.createVaPreparingMessage())
    }

    fun onVaBankingDetailsResolved(bankCredentials: BankCredentials) {
        // Bank credentials just loaded on retry — show the requisites straight away ([REDACTED_TASK_KEY]),
        // instead of the intro deposit sheet that would need another "Show details" tap.
        onShowVirtualAccountRequisites(bankCredentials)
    }

    fun onContactSupportClicked() {
        analytics.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
        val customerId = currentStatus.value.ifLoadedOrNull { it.customerId } ?: return
        modelScope.launch {
            sendFeedbackEmailUseCase.invoke(
                type = FeedbackEmailType.Visa.FeatureIsBeta(
                    walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    fun onVirtualAccountOrderCreated() {
        analytics.send(TangemPayAnalyticsEvents.VaSuccessScreenActivation())
        bottomSheetNavigation.dismiss()
        router.push(TangemPayCardDetailsInnerRoute.VirtualAccountDepositSuccess)
    }

    fun onShowVirtualAccountRequisites(bankCredentials: BankCredentials) {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.VirtualAccountRequisites(
                userWalletId = userWalletId,
                bankCredentials = bankCredentials,
            ),
        )
    }

    fun onVaBankingDetailsShown() {
        analytics.send(TangemPayAnalyticsEvents.VaBankingDetailsShowed())
    }

    fun onVaShareDetailsClicked() {
        analytics.send(TangemPayAnalyticsEvents.VaShareDetailsButtonClicked())
    }

    fun onVaFieldCopied(field: String) {
        analytics.send(TangemPayAnalyticsEvents.VaCopyFieldClicked(field))
    }

    override fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    override fun onSelectAvailable(networkRawId: String) {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayCardNavigation.PaymentReceive(walletId = userWalletId, networkRawId = networkRawId),
        )
    }

    override fun onSelectDisabled() {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(TangemPayCardNavigation.OtherNetworks)
    }

    override fun onDismiss() {
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
        }.saveIn(changeFrozenStateJobHolder)
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
        }.saveIn(changeFrozenStateJobHolder)
    }

    private fun onClickAddToWallet() {
        val card = selectedCard() ?: return
        analytics.send(TangemPayAnalyticsEvents.AddToWalletClicked())
        router.push(TangemPayCardDetailsInnerRoute.AddToWallet(card))
    }

    private fun onClickCloseBanner() {
        modelScope.launch {
            cardDetailsRepository.setAddToWalletAsDone(userWalletId)
            uiState.update { it.copy(addToWalletBlockState = null) }
            shouldShowGooglePayBanner.update { false }
        }
    }

    override fun onClickChangePin() {
        bottomSheetNavigation.dismiss()
        val card = selectedCard() ?: return
        router.push(TangemPayCardDetailsInnerRoute.ChangePIN(card))
    }

    override fun onDismissViewPin() {
        bottomSheetNavigation.dismiss()
    }
}

private fun List<TangemPayCard>.isLastCard(): Boolean = count { it.state == TangemPayCardState.Active } <= 1