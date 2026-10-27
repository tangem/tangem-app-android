package com.tangem.features.tangempay.withdraw.hub.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.recents.TokenTransferJournal
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.recents.GetRecentTokenTransfersUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSourcesObserver
import com.tangem.features.tangempay.withdraw.buildWithdrawSwapRoute
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayWithdrawHubModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val uiMessageSender: UiMessageSender,
    private val getRecentBankAccountsUseCase: GetRecentBankAccountsUseCase,
    private val getRecentTokenTransfersUseCase: GetRecentTokenTransfersUseCase,
    private val getBankOfframpOptionsUseCase: GetBankOfframpOptionsUseCase,
    private val bankWithdrawSourcesObserver: BankWithdrawSourcesObserver,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<Params>()

    private val productInstanceId: String? = when (val account = params.sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> null
        is BankWithdrawSourceAccount.Money -> account.productInstanceId
    }

    private val isBankTransferOffered: Boolean = when (val account = params.sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> true
        is BankWithdrawSourceAccount.Money -> account.productInstanceId != null
    }

    private val cryptoCurrency: CryptoCurrency.Token = tangemPayCurrencyFactory.create(params.userWalletId)

    private val umFactory = TangemPayWithdrawHubUMFactory(
        sourceAccount = params.sourceAccount,
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onWithinPortfolioClick = ::onWithinPortfolioClick,
            onRecentTokenClick = ::onRecentTokenClick,
            onToPersonalAccountClick = {
                onBankOptionClick(
                    paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.ToPersonalAccountClicked(),
                    entry = BankWithdrawEntry.NewPersonal,
                )
            },
            onToBusinessAccountClick = {
                onBankOptionClick(
                    paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.ToBusinessAccountClicked(),
                    entry = BankWithdrawEntry.NewBusiness,
                )
            },
            onRecentRecipientClick = { bankAccountId ->
                onBankOptionClick(
                    paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactClicked(),
                    entry = BankWithdrawEntry.Saved(bankAccountId = bankAccountId),
                )
            },
            onSeeAllClick = {
                onBankOptionClick(
                    paymentAccountEvent = TangemPayAnalyticsEvents.Offramp.SavedContactsSeeAllClicked(),
                    entry = BankWithdrawEntry.SeeAll,
                )
            },
            onRetryRecentsClick = ::loadRecents,
        ),
    )

    private val state = MutableStateFlow(
        WithdrawHubState(options = if (isBankTransferOffered) OptionsState.Loading else OptionsState.NotOffered),
    )

    private val recentsJobHolder = JobHolder()
    private val optionsJobHolder = JobHolder()

    val uiState: StateFlow<TangemPayWithdrawHubUM>
        field = MutableStateFlow(umFactory.create(state.value))

    init {
        state
            .onEach { uiState.value = umFactory.create(it) }
            .launchIn(modelScope)
        if (isBankTransferOffered) {
            loadRecents()
            loadOptions()
        }
        if (params.sourceAccount is BankWithdrawSourceAccount.Payment) {
            analytics.send(TangemPayAnalyticsEvents.Offramp.BankTransferSheetOpened())
            observeTokenRecents()
        }
    }

    private fun loadRecents() {
        state.update { it.copy(recents = RecentsState.Loading) }
        modelScope.launch {
            val recents = getRecentBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = { RecentsState.Failed },
                ifRight = { RecentsState.Loaded(accounts = it.recents, hasMoreAccounts = it.hasMoreAccounts) },
            )
            state.update { it.copy(recents = recents) }
        }.saveIn(recentsJobHolder)
    }

    private fun loadOptions() {
        state.update { it.copy(options = OptionsState.Loading) }
        modelScope.launch {
            getBankOfframpOptionsUseCase(params.userWalletId, productInstanceId).fold(
                ifLeft = { error ->
                    TangemLogger.withTag(TAG).e("Failed to load bank offramp options: $error")
                    state.update { it.copy(options = OptionsState.Unavailable) }
                },
                ifRight = { options -> observeAvailableSources(options) },
            )
        }.saveIn(optionsJobHolder)
    }

    private fun observeTokenRecents() {
        getRecentTokenTransfersUseCase(
            userWalletId = params.userWalletId,
            journal = TokenTransferJournal.PAYMENT_ACCOUNT,
        )
            .onEach { transfers ->
                state.update { it.copy(tokenRecents = TokenRecentsState.Loaded(transfers = transfers)) }
            }
            .launchIn(modelScope)
    }

    /** Balances move while the hub is open, so the tokens the wizard may start from are resolved on every update. */
    private suspend fun observeAvailableSources(options: BankOfframpOptions) {
        bankWithdrawSourcesObserver.observe(params.sourceAccount, options).collect { resolved ->
            state.update { current ->
                current.copy(options = resolved?.let(OptionsState::Ready) ?: OptionsState.Unavailable)
            }
        }
    }

    fun onWithdrawSent() {
        modelScope.launch {
            when (params.sourceAccount) {
                is BankWithdrawSourceAccount.Payment -> paymentAccountStatusFetcher(params.userWalletId)
                is BankWithdrawSourceAccount.Money -> withContext(NonCancellable) {
                    delay(ON_CHAIN_REFRESH_DELAY_MILLIS)
                    moneyAccountStatusFetcher(params.userWalletId)
                }
            }.onLeft { error ->
                TangemLogger.withTag(TAG).e("Failed to refresh the source account after a bank withdraw: $error")
            }
        }
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun onWithinPortfolioClick() = openWithdrawSwap()

    private fun onRecentTokenClick(currencyId: String) {
        val loaded = state.value.tokenRecents as? TokenRecentsState.Loaded ?: return
        val transfer = loaded.transfers.firstOrNull { it.currency.id.value == currencyId } ?: return

        openWithdrawSwap(toCryptoCurrency = transfer.currency)
    }

    private fun openWithdrawSwap(toCryptoCurrency: CryptoCurrency? = null) {
        modelScope.launch {
            val hasActiveWithdrawal = tangemPayWithdrawRepository.hasWithdrawOrder(params.userWalletId)
            if (hasActiveWithdrawal) {
                uiMessageSender.send(
                    TangemPayMessagesFactory.createErrorMessage(
                        errorType = TangemPayDetailsErrorType.WithdrawInProgress,
                    ),
                )
            } else {
                router.push(
                    buildWithdrawSwapRoute(
                        userWalletId = params.userWalletId,
                        cryptoCurrency = cryptoCurrency,
                        toCryptoCurrency = toCryptoCurrency,
                    ),
                )
            }
        }
    }

    private fun onBankOptionClick(paymentAccountEvent: AnalyticsEvent, entry: BankWithdrawEntry) {
        analytics.send(
            when (params.sourceAccount) {
                is BankWithdrawSourceAccount.Payment -> paymentAccountEvent
                is BankWithdrawSourceAccount.Money -> MoneyAccountAnalyticsEvents.WithdrawOptionSelected(
                    method = MoneyAccountAnalyticsEvents.WithdrawMethod.BANK_TRANSFER,
                )
            },
        )
        openBankWithdraw(entry = entry)
    }

    /**
     * The wizard prices the whole withdrawal from these options, so it gets them as a flow: balances move while it
     * is open. The flow keeps the last prepared value if the account stops qualifying mid-flow — the order is
     * refused by the backend anyway, and blanking the open wizard would be worse.
     */
    private fun openBankWithdraw(entry: BankWithdrawEntry) {
        val options = (state.value.options as? OptionsState.Ready)?.options ?: return
        val liveOptions = state
            .map { (it.options as? OptionsState.Ready)?.options }
            .filterNotNull()
            .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = options)
        params.onOpenBankWithdraw(entry, liveOptions)
    }

    data class Params(
        val sourceAccount: BankWithdrawSourceAccount,
        val onOpenBankWithdraw: (entry: BankWithdrawEntry, options: StateFlow<BankWithdrawOptions>) -> Unit,
    ) {
        val userWalletId: UserWalletId get() = sourceAccount.userWalletId
    }
}

private const val ON_CHAIN_REFRESH_DELAY_MILLIS = 11_000L

private const val TAG = "TangemPayWithdrawHubModel"