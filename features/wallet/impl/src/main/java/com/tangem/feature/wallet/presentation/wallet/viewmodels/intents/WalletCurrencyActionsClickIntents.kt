package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.blockchain.common.address.AddressType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.models.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.models.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

interface WalletCurrencyActionsClickIntents {

    fun onSendClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus, unavailabilityReason: ScenarioUnavailabilityReason)

    fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference?

    fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onExploreClick()
}

@Suppress("LongParameterList", "LargeClass")
@ViewModelScoped
internal class WalletCurrencyActionsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val getYieldUseCase: GetYieldUseCase,
) : BaseWalletClickIntents(), WalletCurrencyActionsClickIntents {

    override fun onSendClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSend(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))
        viewModelScope.launch(dispatchers.main) {
            val maybeFeeCurrencyStatus =
                getFeePaidCryptoCurrencyStatusSyncUseCase(userWallet.walletId, cryptoCurrencyStatus).getOrNull()
            when (val currency = cryptoCurrencyStatus.currency) {
                is CryptoCurrency.Coin -> sendCoin(cryptoCurrencyStatus, userWallet, maybeFeeCurrencyStatus)
                is CryptoCurrency.Token -> sendToken(
                    cryptoCurrency = currency,
                    cryptoCurrencyStatus = cryptoCurrencyStatus.value,
                    feeCurrencyStatus = maybeFeeCurrencyStatus,
                    userWallet = userWallet,
                )
            }
        }
    }

    private fun sendCoin(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        userWallet: UserWallet,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ) {
        reduxStateHolder.dispatch(
            action = TradeCryptoAction.SendCoin(
                userWallet = userWallet,
                coinStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCurrencyStatus,
            ),
        )
    }

    private fun sendToken(
        cryptoCurrency: CryptoCurrency.Token,
        cryptoCurrencyStatus: CryptoCurrencyStatus.Value,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        userWallet: UserWallet,
    ) {
        viewModelScope.launch(dispatchers.main) {
            getNetworkCoinStatusUseCase(
                userWalletId = userWallet.walletId,
                networkId = cryptoCurrency.network.id,
                derivationPath = cryptoCurrency.network.derivationPath,
                isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .take(count = 1)
                .collectLatest {
                    it.onRight { coinStatus ->
                        reduxStateHolder.dispatch(
                            action = TradeCryptoAction.SendToken(
                                userWallet = userWallet,
                                tokenCurrency = cryptoCurrency,
                                tokenFiatRate = cryptoCurrencyStatus.fiatRate,
                                coinFiatRate = coinStatus.value.fiatRate,
                                feeCurrencyStatus = feeCurrencyStatus,
                            ),
                        )
                    }
                }
        }
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWalletId = stateHolder.getSelectedWalletId()

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonReceive(cryptoCurrencyStatus.currency.symbol),
        )

        analyticsEventHandler.send(event = TokenReceiveAnalyticsEvent.ReceiveScreenOpened)

        stateHolder.showBottomSheet(
            createReceiveBottomSheetContent(
                currency = cryptoCurrencyStatus.currency,
                addresses = cryptoCurrencyStatus.value.networkAddress?.availableAddresses ?: return,
            ),
            userWalletId,
        )
    }

    override fun onCopyAddressLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus): TextReference? {
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return null
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val addresses = networkAddress.availableAddresses.mapToAddressModels(cryptoCurrency).toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return null

        vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
        clipboardManager.setText(text = defaultAddress)
        analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
        return resourceReference(R.string.wallet_notification_address_copied)
    }

    private fun createReceiveBottomSheetContent(
        currency: CryptoCurrency,
        addresses: Set<NetworkAddress.Address>,
    ): TangemBottomSheetConfigContent {
        return TokenReceiveBottomSheetConfig(
            name = currency.name,
            symbol = currency.symbol,
            network = currency.network.name,
            addresses = addresses.mapToAddressModels(currency).toImmutableList(),
            onCopyClick = {
                analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(currency.symbol))
            },
            onShareClick = {
                analyticsEventHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(currency.symbol))
            },
        )
    }

    override fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonCopyAddress(cryptoCurrencyStatus.currency.symbol),
        )

        viewModelScope.launch(dispatchers.main) {
            walletManagersFacade.getAddress(
                userWalletId = stateHolder.getSelectedWalletId(),
                network = cryptoCurrencyStatus.currency.network,
            )
                .find { it.type == AddressType.Default }
                ?.value
                ?.let {
                    stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))

                    walletEventSender.send(
                        event = WalletEvent.CopyAddress(address = it),
                    )
                }
        }
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrencyStatus.currency.symbol),
        )

        viewModelScope.launch(dispatchers.main) {
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = getHideTokeAlertConfig(stateHolder.getSelectedWalletId(), cryptoCurrencyStatus),
                ),
            )
        }
    }

    private suspend fun getHideTokeAlertConfig(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): WalletAlertState.DefaultAlert {
        val currency = cryptoCurrencyStatus.currency
        return if (currency is CryptoCurrency.Coin && !isCryptoCurrencyCoinCouldHide(userWalletId, currency)) {
            WalletAlertState.DefaultAlert(
                title = resourceReference(
                    id = R.string.token_details_unable_hide_alert_title,
                    formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                ),
                message = resourceReference(
                    id = R.string.token_details_unable_hide_alert_message,
                    formatArgs = WrappedList(
                        listOf(
                            cryptoCurrencyStatus.currency.name,
                            cryptoCurrencyStatus.currency.symbol,
                            cryptoCurrencyStatus.currency.network.name,
                        ),
                    ),
                ),
                onConfirmClick = null,
            )
        } else {
            WalletAlertState.DefaultAlert(
                title = resourceReference(
                    id = R.string.token_details_hide_alert_title,
                    formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                ),
                message = resourceReference(R.string.token_details_hide_alert_message),
                onConfirmClick = { onPerformHideToken(cryptoCurrencyStatus) },
            )
        }
    }

    override fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWalletId = stateHolder.getSelectedWalletId()

        viewModelScope.launch(dispatchers.io) {
            removeCurrencyUseCase(userWalletId, cryptoCurrencyStatus.currency)
                .fold(
                    ifLeft = {
                        walletEventSender.send(
                            event = WalletEvent.ShowError(text = resourceReference(R.string.common_error)),
                        )
                    },
                    ifRight = {
                        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWalletId))
                    },
                )
        }
    }

    override fun onSellClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSell(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    action = TradeCryptoAction.Sell(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        appCurrencyCode = getSelectedAppCurrencyUseCase.unwrap().code,
                    ),
                )
            }
        }
    }

    override fun onBuyClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    TradeCryptoAction.Buy(
                        userWallet = userWallet,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        appCurrencyCode = getSelectedAppCurrencyUseCase.unwrap().code,
                    ),
                )
            }
        }
    }

    override fun onSwapClick(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        unavailabilityReason: ScenarioUnavailabilityReason,
    ) {
        analyticsEventHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonExchange(cryptoCurrencyStatus.currency.symbol),
        )

        if (handleUnavailabilityReason(unavailabilityReason)) return

        reduxStateHolder.dispatch(TradeCryptoAction.Swap(cryptoCurrencyStatus.currency))
    }

    override fun onExploreClick() {
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    override fun onStakeClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return
        stateHolder.update(CloseBottomSheetTransformer(userWalletId = userWallet.walletId))

        viewModelScope.launch {
            val userWalletId = stateHolder.getSelectedWalletId()
            val cryptoCurrency = cryptoCurrencyStatus.currency
            val yield = getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).getOrElse {
                error("Staking is unavailable for ${cryptoCurrency.name}")
            }

            reduxStateHolder.dispatch(
                TradeCryptoAction.Stake(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    yield = yield,
                ),
            )
        }
    }

    private fun openExplorer() {
        val userWalletId = stateHolder.getSelectedWalletId()

        viewModelScope.launch(dispatchers.main) {
            val currencyStatus = getPrimaryCurrencyStatusUpdatesUseCase.unwrap(userWalletId) ?: return@launch

            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    showChooseAddressBottomSheet(userWalletId, addresses.availableAddresses, currencyStatus.currency)
                }
                is NetworkAddress.Single -> {
                    router.openUrl(
                        url = getExploreUrlUseCase(
                            userWalletId = userWalletId,
                            currency = currencyStatus.currency,
                            addressType = AddressType.Default,
                        ),
                    )
                }
                null -> Unit
            }
        }
    }

    private fun showChooseAddressBottomSheet(
        userWalletId: UserWalletId,
        addresses: Set<NetworkAddress.Address>,
        currency: CryptoCurrency,
    ) {
        stateHolder.showBottomSheet(
            ChooseAddressBottomSheetConfig(
                addressModels = addresses.mapToAddressModels(currency).toImmutableList(),
                onClick = {
                    onAddressTypeSelected(
                        userWalletId = userWalletId,
                        currency = currency,
                        addressModel = it,
                    )
                },
            ),
            userWalletId,
        )
    }

    private fun onAddressTypeSelected(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        addressModel: AddressModel,
    ) {
        viewModelScope.launch(dispatchers.main) {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    currency = currency,
                    addressType = AddressType.valueOf(addressModel.type.name),
                ),
            )

            stateHolder.update(
                CloseBottomSheetTransformer(userWalletId = userWalletId),
            )
        }
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        val cardId = getSelectedWalletSyncUseCase.unwrap()?.cardId ?: return

        if (isDemoCardUseCase(cardId = cardId)) {
            stateHolder.update(CloseBottomSheetTransformer(userWalletId = stateHolder.getSelectedWalletId()))

            walletEventSender.send(
                event = WalletEvent.ShowError(
                    text = resourceReference(id = R.string.alert_demo_feature_disabled),
                ),
            )
        } else {
            action()
        }
    }

    private fun handleUnavailabilityReason(unavailabilityReason: ScenarioUnavailabilityReason): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false

        val unavailabilityReasonText = getUnavailabilityReasonText(unavailabilityReason)

        viewModelScope.launch(dispatchers.main) {
            walletEventSender.send(
                event = WalletEvent.ShowAlert(
                    state = WalletAlertState.DefaultAlert(
                        title = null,
                        message = unavailabilityReasonText,
                        onConfirmClick = null,
                    ),
                ),
            )
        }

        return true
    }

    private fun getUnavailabilityReasonText(unavailabilityReason: ScenarioUnavailabilityReason): TextReference {
        return when (unavailabilityReason) {
            is ScenarioUnavailabilityReason.StakingUnavailable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_staking_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.PendingTransaction -> {
                when (unavailabilityReason.withdrawalScenario) {
                    ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                        id = R.string.token_button_unavailability_reason_pending_transaction_send,
                        formatArgs = wrappedList(unavailabilityReason.networkName),
                    )
                    ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                        id = R.string.token_button_unavailability_reason_pending_transaction_sell,
                        formatArgs = wrappedList(unavailabilityReason.networkName),
                    )
                }
            }
            is ScenarioUnavailabilityReason.EmptyBalance -> {
                when (unavailabilityReason.withdrawalScenario) {
                    ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                        id = R.string.token_button_unavailability_reason_empty_balance_send,
                    )
                    ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                        id = R.string.token_button_unavailability_reason_empty_balance_sell,
                    )
                }
            }
            is ScenarioUnavailabilityReason.BuyUnavailable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_buy_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.NotExchangeable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_not_exchangeable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            is ScenarioUnavailabilityReason.NotSupportedBySellService -> {
                resourceReference(
                    id = R.string.token_button_unavailability_reason_sell_unavailable,
                    formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
                )
            }
            ScenarioUnavailabilityReason.Unreachable -> {
                resourceReference(
                    id = R.string.token_button_unavailability_generic_description,
                )
            }
            ScenarioUnavailabilityReason.UnassociatedAsset -> resourceReference(
                id = R.string.warning_receive_blocked_hedera_token_association_required_message,
            )
            ScenarioUnavailabilityReason.None -> {
                throw IllegalArgumentException("The unavailability reason must be other than None")
            }
        }
    }
}
