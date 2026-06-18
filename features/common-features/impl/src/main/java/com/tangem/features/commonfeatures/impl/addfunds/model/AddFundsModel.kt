package com.tangem.features.commonfeatures.impl.addfunds.model

import androidx.compose.runtime.Immutable
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.commonfeatures.api.addfunds.AddFundsComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddWallet
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.api.tokenactions.BottomAction
import com.tangem.features.commonfeatures.impl.addfunds.analytics.AddFundsAnalyticsEvent
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.userportfolio.state.UserPortfolioStateController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class AddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    userPortfolioStateControllerFactory: UserPortfolioStateController.Factory,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), TokenActionsComponent.Callbacks {

    private val params = paramsContainer.require<AddFundsComponent.Params>()
    val launchMode: AddFundsComponent.LaunchMode = params.launchMode

    private val routeStack = MutableStateFlow(listOf<UiRoute>(UiRoute.Loading))

    val uiRoute: StateFlow<UiRoute> = routeStack
        .map { it.last() }
        .distinctUntilChanged()
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = UiRoute.Loading)

    val canGoBack: StateFlow<Boolean> = routeStack
        .map { it.size > 1 }
        .distinctUntilChanged()
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = false)

    private val tokenActionsTrigger = MutableStateFlow<TokenActionsRequest?>(null)
    private val filteredEntries = MutableStateFlow<List<FilteredEntry>>(emptyList())

    val currentBottomAction: MutableStateFlow<BottomAction> =
        MutableStateFlow(BottomAction.None)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tokenActionsData: Flow<CryptoCurrencyData> = tokenActionsTrigger
        .filterNotNull()
        .flatMapLatest { request ->
            combine(
                getCryptoCurrencyActionsUseCase(
                    accountId = request.account.account.accountId,
                    currency = request.status.currency,
                ),
                isAccountsModeEnabledUseCase(),
            ) { actionsState, isAccountMode ->
                CryptoCurrencyData(
                    userWallet = request.userWallet,
                    status = actionsState.cryptoCurrencyStatus,
                    actions = actionsState.states,
                    isAccountMode = isAccountMode,
                    account = request.account,
                )
            }
        }

    val chooseTokenBridge: ChooseTokenBridge by lazy {
        chooseTokenBridgeFactory.create(
            modelScope = modelScope,
            settings = ChooseTokenBridge.Settings.AddFunds,
            analyticsPayload = setOf(ChooseTokenAnalyticsPayload.ScreensSources(SCREEN_SOURCE)),
        )
    }

    val userPortfolioStateController: UserPortfolioStateController = userPortfolioStateControllerFactory.create(
        modelScope = modelScope,
        onTokenSelected = { result ->
            openTokenActions(
                request = TokenActionsRequest(
                    userWallet = result.wallet,
                    account = result.account,
                    status = result.addedCurrency,
                ),
                bottomAction = BottomAction.None,
            )
        },
    )

    init {
        when (val mode = launchMode) {
            is AddFundsComponent.LaunchMode.ChooseToken -> initChooseToken(mode)
            is AddFundsComponent.LaunchMode.TokenActionsOnly -> initTokenActionsOnly(mode)
            is AddFundsComponent.LaunchMode.FilteredByRawId -> initFilteredByRawId(mode)
        }
    }

    fun onBack() {
        routeStack.update { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
    }

    fun onDismiss() = params.onDismiss()

    override fun onBottomActionClick(bottomAction: BottomAction) {
        val request = tokenActionsTrigger.value
        if (bottomAction == BottomAction.GoToToken && request != null) {
            appRouter.push(
                AppRoute.CurrencyDetails(
                    userWalletId = request.userWallet.walletId,
                    currency = request.status.currency,
                ),
            )
        }
        params.onDismiss()
    }

    override fun onQuickActionClick(action: TokenActionsBSContentUM.Action, shouldDismiss: Boolean) {
        val event = when (action) {
            TokenActionsBSContentUM.Action.Buy -> AddFundsAnalyticsEvent.ButtonBuy()
            TokenActionsBSContentUM.Action.Exchange -> AddFundsAnalyticsEvent.ButtonSwap()
            TokenActionsBSContentUM.Action.Receive -> AddFundsAnalyticsEvent.ButtonReceive()
            else -> null
        }
        event?.let { analyticsEventHandler.send(it) }
        if (shouldDismiss) {
            params.onDismiss()
        }
    }

    fun buildAvailableToAddDataForChooser(): AvailableToAddData {
        val byWallet = filteredEntries.value.groupBy { it.userWallet.walletId }
        return AvailableToAddData(
            availableToAddWallets = byWallet.mapValues { (_, entries) ->
                AvailableToAddWallet(
                    userWallet = entries.first().userWallet,
                    accounts = entries.map { it.account }.distinct(),
                    availableNetworks = emptySet(),
                    availableToAddAccounts = emptyMap(),
                )
            },
        )
    }

    private fun initChooseToken(mode: AddFundsComponent.LaunchMode.ChooseToken) {
        chooseTokenBridge.selectWalletTab(mode.userWalletId)
        analyticsEventHandler.send(
            AddFundsAnalyticsEvent.MethodScreenOpened(source = AddFundsAnalyticsEvent.SOURCE_MAIN_SCREEN),
        )
        replaceRoot(UiRoute.ChooseToken)
        modelScope.launch {
            chooseTokenBridge.onCurrencyChosen.receiveAsFlow().collect(::openTokenActionsFromBridge)
        }
        modelScope.launch {
            chooseTokenBridge.onClose.receiveAsFlow().collect { params.onDismiss() }
        }
    }

    private fun initTokenActionsOnly(mode: AddFundsComponent.LaunchMode.TokenActionsOnly) {
        modelScope.launch {
            val wallet = getUserWalletUseCase.invokeFlow(mode.userWalletId)
                .mapNotNull { it.getOrNull() }
                .first()
            val match = multiAccountStatusListSupplier()
                .first()
                .firstOrNull { it.userWalletId == mode.userWalletId }
                ?.accountStatuses
                ?.filterCryptoPortfolio()
                ?.firstNotNullOfOrNull { accountStatus ->
                    accountStatus.tokenList.flattenCurrencies()
                        .firstOrNull { it.currency.id == mode.currency.id }
                        ?.let { accountStatus to it }
                }
                ?: run {
                    params.onDismiss()
                    return@launch
                }
            tokenActionsTrigger.value = TokenActionsRequest(wallet, match.first, match.second)
            replaceRoot(UiRoute.TokenActions)
        }
    }

    private fun initFilteredByRawId(mode: AddFundsComponent.LaunchMode.FilteredByRawId) {
        modelScope.launch {
            val entries = collectFilteredEntries(mode.rawCurrencyId)
            when (entries.size) {
                0 -> params.onDismiss()
                1 -> {
                    val entry = entries.first()
                    tokenActionsTrigger.value = TokenActionsRequest(entry.userWallet, entry.account, entry.status)
                    replaceRoot(UiRoute.TokenActions)
                }
                else -> {
                    filteredEntries.value = entries
                    replaceRoot(UiRoute.UserPortfolio)
                }
            }
        }
    }

    private suspend fun collectFilteredEntries(rawCurrencyId: CryptoCurrency.RawID): List<FilteredEntry> {
        val accountLists = multiAccountStatusListSupplier().first()
        return accountLists.flatMap { accountStatusList ->
            val wallet = getUserWalletUseCase.invokeFlow(accountStatusList.userWalletId)
                .mapNotNull { it.getOrNull() }
                .firstOrNull()
                ?: return@flatMap emptyList()
            accountStatusList.accountStatuses.filterCryptoPortfolio().flatMap { accountStatus ->
                accountStatus.tokenList.flattenCurrencies()
                    .filter { status ->
                        val id = status.currency.id.rawCurrencyId ?: return@filter false
                        getTokenIdIfL2Network(id.value) == rawCurrencyId.value
                    }
                    .map { status -> FilteredEntry(wallet, accountStatus, status) }
            }
        }
    }

    private fun openTokenActionsFromBridge(result: ChooseTokenResult) {
        val account = result.account as? AccountStatus.CryptoPortfolio ?: return
        openTokenActions(
            request = TokenActionsRequest(result.wallet, account, result.currency),
            bottomAction = if (result.wasJustAdded) {
                BottomAction.GoToToken
            } else {
                BottomAction.None
            },
        )
    }

    private fun openTokenActions(request: TokenActionsRequest, bottomAction: BottomAction) {
        tokenActionsTrigger.value = request
        currentBottomAction.value = bottomAction
        pushRoute(UiRoute.TokenActions)
    }

    private fun replaceRoot(route: UiRoute) {
        routeStack.value = listOf(route)
    }

    private fun pushRoute(route: UiRoute) {
        routeStack.update { it + route }
    }

    @Immutable
    sealed interface UiRoute {
        data object Loading : UiRoute
        data object ChooseToken : UiRoute
        data object UserPortfolio : UiRoute
        data object TokenActions : UiRoute
    }

    private data class TokenActionsRequest(
        val userWallet: UserWallet,
        val account: AccountStatus.CryptoPortfolio,
        val status: CryptoCurrencyStatus,
    )

    private data class FilteredEntry(
        val userWallet: UserWallet,
        val account: AccountStatus.CryptoPortfolio,
        val status: CryptoCurrencyStatus,
    )

    private companion object {
        const val SCREEN_SOURCE = "AddFunds"
    }
}