package com.tangem.features.tangempay.withdraw.hub.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.common.balanceOrNull
import com.tangem.features.tangempay.common.networksOrNull
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptionsFactory
import com.tangem.features.tangempay.withdraw.buildWithdrawSwapRoute
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
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
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
    private val markBankAccountUsedUseCase: MarkBankAccountUsedUseCase,
    private val getBankOfframpOptionsUseCase: GetBankOfframpOptionsUseCase,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    private val params = paramsContainer.require<Params>()

    private val cryptoCurrency: CryptoCurrency.Token = tangemPayCurrencyFactory.create(params.userWalletId)

    private val umFactory = TangemPayWithdrawHubUMFactory(
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onWithinPortfolioClick = ::onWithinPortfolioClick,
            onToPersonalAccountClick = { openBankWithdraw(entry = BankWithdrawEntry.NewPersonal) },
            onToBusinessAccountClick = { openBankWithdraw(entry = BankWithdrawEntry.NewBusiness) },
            onRecentRecipientClick = ::onRecentRecipientClick,
            onSeeAllClick = { openBankWithdraw(entry = BankWithdrawEntry.SeeAll) },
            onRetryRecentsClick = ::loadRecents,
            onRetryBankSectionClick = ::onRetryBankSection,
        ),
    )

    private val state = MutableStateFlow(WithdrawHubState())

    private val recentsJobHolder = JobHolder()
    private val optionsJobHolder = JobHolder()

    val uiState: StateFlow<TangemPayWithdrawHubUM>
        field = MutableStateFlow(umFactory.create(state.value))

    init {
        state
            .onEach { uiState.value = umFactory.create(it) }
            .launchIn(modelScope)
        loadRecents()
        loadOptions()
    }

    private fun loadRecents() {
        state.update { it.copy(recents = RecentsState.Loading) }
        modelScope.launch {
            val recents = getRecentBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = { RecentsState.Failed },
                ifRight = { accounts ->
                    RecentsState.Loaded(
                        accounts = accounts,
                        hasSavedAccounts = accounts.isNotEmpty() || hasSavedAccounts(),
                    )
                },
            )
            state.update { it.copy(recents = recents) }
        }.saveIn(recentsJobHolder)
    }

    /** With an empty usage log "See all" is the only way to reach recipients saved earlier or on another device. */
    private suspend fun hasSavedAccounts(): Boolean = getBankAccountsUseCase(params.userWalletId).fold(
        ifLeft = { false },
        ifRight = { accounts -> accounts.isNotEmpty() },
    )

    private fun loadOptions() {
        state.update { it.copy(options = OptionsState.Loading) }
        modelScope.launch {
            getBankOfframpOptionsUseCase(params.userWalletId).fold(
                ifLeft = { error ->
                    TangemLogger.withTag(TAG).e("Failed to load bank offramp options: $error")
                    state.update { it.copy(options = OptionsState.Unavailable) }
                },
                ifRight = { options -> observeAvailableSources(options) },
            )
        }.saveIn(optionsJobHolder)
    }

    /** Balances move while the hub is open, so the tokens the wizard may start from are resolved on every update. */
    private suspend fun observeAvailableSources(options: BankOfframpOptions) {
        paymentAccountStatusSupplier(params.userWalletId).collect { status ->
            val networks = status.networksOrNull()
            if (networks == null) {
                // Only a loading account is still going to produce networks; any other state without them never
                // will, and leaving the section in Loading would hide the retry behind an endless shimmer.
                if (status.value !is PaymentAccountStatusValue.Loading) {
                    state.update { it.copy(options = OptionsState.Unavailable) }
                }
                return@collect
            }
            val resolved = BankWithdrawOptionsFactory.create(
                options = options,
                networks = networks,
                balanceCurrencyCode = status.balanceOrNull()?.fiatBalance?.currency,
            )
            state.update { current ->
                current.copy(options = resolved?.let(OptionsState::Ready) ?: OptionsState.Unavailable)
            }
        }
    }

    private fun onRetryBankSection() {
        loadOptions()
        loadRecents()
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun onWithinPortfolioClick() {
        modelScope.launch {
            val hasActiveWithdrawal = tangemPayWithdrawRepository.hasWithdrawOrder(params.userWalletId)
            if (hasActiveWithdrawal) {
                uiMessageSender.send(
                    TangemPayMessagesFactory.createErrorMessage(
                        errorType = TangemPayDetailsErrorType.WithdrawInProgress,
                    ),
                )
            } else {
                router.pop()
                router.push(buildWithdrawSwapRoute(userWalletId = params.userWalletId, cryptoCurrency = cryptoCurrency))
            }
        }
    }

    private fun onRecentRecipientClick(bankAccountId: String) {
        modelScope.launch {
            markBankAccountUsedUseCase(userWalletId = params.userWalletId, bankAccountId = bankAccountId)
            openBankWithdraw(entry = BankWithdrawEntry.Saved(bankAccountId = bankAccountId))
        }
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
        val userWalletId: UserWalletId,
        val onOpenBankWithdraw: (entry: BankWithdrawEntry, options: StateFlow<BankWithdrawOptions>) -> Unit,
    )
}

private const val TAG = "TangemPayWithdrawHubModel"