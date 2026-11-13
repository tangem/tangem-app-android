package com.tangem.features.earn.impl.getfunds

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.backup.BackupErrorWarningSender
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.common.ui.tokens.getUnavailabilityReasonTitle
import com.tangem.core.analytics.models.AnalyticsParam
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
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
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
        uiState.value = EarnGetTokenSheetConverter.convert(
            currency = request.cryptoCurrency,
            actions = data.actions,
            onActionClick = { action -> onActionClick(action, data) },
            onDismiss = params.onDismiss,
        )
    }

    private fun onActionClick(action: EarnGetTokenAction, data: EarnGetTokenData) {
        val reason = action.reasonIn(data.actions) ?: return
        if (reason != ScenarioUnavailabilityReason.None) {
            uiMessageSender.send(
                DialogMessage(
                    message = reason.getUnavailabilityReasonText(),
                    title = reason.getUnavailabilityReasonTitle(),
                ),
            )
            return
        }
        when (action) {
            EarnGetTokenAction.Swap -> openSwap(data)
            EarnGetTokenAction.Buy -> warnAboutBackupErrorOrProceed(data) { openOnramp(data) }
            EarnGetTokenAction.Deposit -> warnAboutBackupErrorOrProceed(data) { openReceive(data) }
        }
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