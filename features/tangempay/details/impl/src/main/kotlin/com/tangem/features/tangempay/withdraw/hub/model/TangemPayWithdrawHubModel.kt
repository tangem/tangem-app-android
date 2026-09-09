package com.tangem.features.tangempay.withdraw.hub.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.buildWithdrawSwapRoute
import com.tangem.features.tangempay.withdraw.hub.TangemPayWithdrawHubComponent
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) : Model() {

    private val params = paramsContainer.require<TangemPayWithdrawHubComponent.Params>()

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
        ),
    )

    private val recentsJobHolder = JobHolder()

    val uiState: StateFlow<TangemPayWithdrawHubUM>
        field = MutableStateFlow(umFactory.create())

    init {
        loadRecents()
    }

    private fun loadRecents() {
        updateRecents(TangemPayWithdrawHubUM.RecentsUM.Loading)
        modelScope.launch {
            val recents = getRecentBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = { umFactory.createRecentsError() },
                ifRight = { recents ->
                    umFactory.createRecents(
                        recents = recents,
                        hasSavedAccounts = recents.isNotEmpty() || hasSavedAccounts(),
                    )
                },
            )
            updateRecents(recents)
        }.saveIn(recentsJobHolder)
    }

    /** With an empty usage log "See all" is the only way to reach recipients saved earlier or on another device. */
    private suspend fun hasSavedAccounts(): Boolean = getBankAccountsUseCase(params.userWalletId).fold(
        ifLeft = { false },
        ifRight = { accounts -> accounts.isNotEmpty() },
    )

    private fun updateRecents(recents: TangemPayWithdrawHubUM.RecentsUM) {
        uiState.update { it.copy(bankSection = it.bankSection.copy(recents = recents)) }
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

    private fun openBankWithdraw(entry: BankWithdrawEntry) {
        router.push(TangemPayAccountDetailsInnerRoute.BankWithdraw(entry = entry))
    }
}