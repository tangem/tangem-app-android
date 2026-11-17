package com.tangem.features.earn.impl.getfunds

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.backup.BackupErrorWarningSender
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.common.ui.tokens.getUnavailabilityReasonTitle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.ManageFundsAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.features.earn.impl.analytics.EarnPickerAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class EarnGetTokenSheetModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val backupErrorWarningSender: BackupErrorWarningSender,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: EarnGetTokenSheetComponent.Params = paramsContainer.require()

    private val request = params.request

    /** `null` until the wallet's actions for the currency are known. */
    val uiState: StateFlow<EarnGetTokenSheetUM?>
        field = MutableStateFlow<EarnGetTokenSheetUM?>(null)

    val receiveConfigs: SharedFlow<TokenReceiveConfig>
        field = MutableSharedFlow<TokenReceiveConfig>(extraBufferCapacity = 1)

    init {
        singleAccountStatusListSupplier(request.userWalletId)
            .map { it.getCryptoCurrencyStatus(request.cryptoCurrency).getOrNull() }
            .flatMapLatest { status ->
                val wallet = userWalletsListRepository.getSyncOrNull(request.userWalletId)
                if (status == null || wallet == null) {
                    flowOf(null)
                } else {
                    getCryptoCurrencyActionsUseCase(wallet, status)
                        .map { EarnGetTokenData(wallet, it.cryptoCurrencyStatus, it.states) }
                }
            }
            .onEach(::onData)
            .launchIn(modelScope)
    }

    private fun onData(data: EarnGetTokenData?) {
        if (data == null) {
            TangemLogger.e("Earn Get token: ${request.cryptoCurrency.symbol} is not in the wallet")
            params.onDismiss()
            return
        }
        if (uiState.value == null) {
            analyticsEventHandler.send(ManageFundsAnalyticsEvent.MethodScreenOpened(AnalyticsParam.ScreensSources.Earn))
            analyticsEventHandler.send(
                EarnPickerAnalyticsEvent.GetAssetSheetOpened(
                    token = request.cryptoCurrency.symbol,
                    source = request.source,
                ),
            )
        }
        uiState.value = EarnGetTokenSheetConverter.convert(
            currency = request.cryptoCurrency,
            actions = data.actions,
            isReceiveCached = data.status.value.sources.networkSource == StatusSource.CACHE,
            onActionClick = { action -> onActionClick(action, data) },
            onDismiss = params.onDismiss,
        )
    }

    private fun onActionClick(action: EarnGetTokenAction, data: EarnGetTokenData) {
        val reason = action.reasonIn(data.actions) ?: return
        if (reason.blocks(action)) {
            uiMessageSender.send(
                DialogMessage(
                    message = reason.getUnavailabilityReasonText(),
                    title = reason.getUnavailabilityReasonTitle(),
                ),
            )
            return
        }
        when (action) {
            EarnGetTokenAction.Swap -> proceed(action) { openSwap(data) }
            EarnGetTokenAction.Buy -> warnAboutBackupErrorOrProceed(data) { proceed(action) { openOnramp(data) } }
            EarnGetTokenAction.Deposit -> warnAboutBackupErrorOrProceed(data) { proceed(action) { openReceive(data) } }
        }
    }

    /**
     * Swap here funds the currency instead of spending it, and a joint account holds back only spending: the shared
     * Add funds sheet lets the same tap through.
     */
    private fun ScenarioUnavailabilityReason.blocks(action: EarnGetTokenAction): Boolean = when (this) {
        ScenarioUnavailabilityReason.None -> false
        ScenarioUnavailabilityReason.JointAccount -> action != EarnGetTokenAction.Swap
        else -> true
    }

    private fun proceed(action: EarnGetTokenAction, open: () -> Unit) {
        val event = when (action) {
            EarnGetTokenAction.Deposit -> ManageFundsAnalyticsEvent.ButtonReceive()
            EarnGetTokenAction.Swap -> ManageFundsAnalyticsEvent.ButtonSwap()
            EarnGetTokenAction.Buy -> ManageFundsAnalyticsEvent.ButtonBuy()
        }
        analyticsEventHandler.send(event)
        analyticsEventHandler.send(
            EarnPickerAnalyticsEvent.GetAssetMethod(token = request.cryptoCurrency.symbol, method = action),
        )
        open()
    }

    private fun warnAboutBackupErrorOrProceed(data: EarnGetTokenData, onProceed: () -> Unit) {
        backupErrorWarningSender.forWallet(scope = modelScope, userWallet = data.userWallet, onProceed = onProceed)
    }

    private fun openSwap(data: EarnGetTokenData) {
        router.push(
            AppRoute.Swap(
                userWalletId = request.userWalletId,
                fromCryptoCurrency = data.status.currency,
                screenSource = AnalyticsParam.ScreensSources.Earn.value,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
            ),
        )
        params.onDismiss()
    }

    /** `OnrampSource` has no Earn value yet: the shared Add funds sheet reports Markets for every entry point. */
    private fun openOnramp(data: EarnGetTokenData) {
        router.push(
            AppRoute.Onramp(
                source = OnrampSource.MARKETS,
                userWalletId = request.userWalletId,
                currency = data.status.currency,
            ),
        )
        params.onDismiss()
    }

    /** The receive sheet opens over this one: the user comes back to the options after closing it. */
    private fun openReceive(data: EarnGetTokenData) {
        modelScope.launch {
            val config = withContext(dispatchers.default) {
                receiveAddressesFactory.create(status = data.status, userWalletId = request.userWalletId)
            }
            if (config != null) receiveConfigs.tryEmit(config)
        }
    }

    private data class EarnGetTokenData(
        val userWallet: UserWallet,
        val status: CryptoCurrencyStatus,
        val actions: List<ActionState>,
    )
}