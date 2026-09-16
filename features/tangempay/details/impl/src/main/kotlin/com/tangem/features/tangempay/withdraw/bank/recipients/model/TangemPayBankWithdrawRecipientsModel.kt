package com.tangem.features.tangempay.withdraw.bank.recipients.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.recipients.TangemPayBankWithdrawRecipientsComponent
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawRecipientsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
    private val markBankAccountUsedUseCase: MarkBankAccountUsedUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawRecipientsComponent.Params>()

    private val umFactory = TangemPayBankWithdrawRecipientsUMFactory(
        callbacks = TangemPayBankWithdrawRecipientsUMFactory.Callbacks(
            onBackClick = router::pop,
            onQueryChange = ::onQueryChange,
            onSearchActiveChange = ::onSearchActiveChange,
            onSearchCloseClick = ::onSearchCloseClick,
            onRecipientClick = ::onRecipientClick,
        ),
    )

    private val state = MutableStateFlow(RecipientsState())

    val uiState: StateFlow<TangemPayBankWithdrawRecipientsUM>
        field = MutableStateFlow(umFactory.create(state.value))

    init {
        state
            .onEach { uiState.value = umFactory.create(it) }
            .launchIn(modelScope)
        loadAccounts()
    }

    private fun loadAccounts() {
        modelScope.launch {
            getBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = ::closeOnLoadFailure,
                ifRight = { accounts -> state.update { it.copy(accounts = accounts) } },
            )
        }
    }

    private fun closeOnLoadFailure(error: BankOfframpError) {
        TangemLogger.withTag(TAG).e("Bank withdraw recipients step closed, failed to load bank accounts: $error")
        uiMessageSender.send(
            TangemPayMessagesFactory.createErrorMessage(errorType = TangemPayDetailsErrorType.Withdraw),
        )
        router.pop()
    }

    private fun onQueryChange(query: String) {
        state.update { it.copy(query = query) }
    }

    private fun onSearchActiveChange(isActive: Boolean) {
        state.update { it.copy(isSearchActive = isActive) }
    }

    private fun onSearchCloseClick() {
        state.update { it.copy(query = "", isSearchActive = false) }
    }

    private fun onRecipientClick(account: BankAccount) {
        modelScope.launch {
            markBankAccountUsedUseCase(userWalletId = params.userWalletId, bankAccountId = account.id)
            params.onRecipientSelected(account)
        }
    }
}

private const val TAG = "TangemPayBankWithdrawRecipientsModel"