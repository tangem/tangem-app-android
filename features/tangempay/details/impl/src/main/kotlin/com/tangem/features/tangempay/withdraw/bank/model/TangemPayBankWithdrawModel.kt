package com.tangem.features.tangempay.withdraw.bank.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.TangemPayBankWithdrawComponent
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormResult
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayBankWithdrawModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val getTangemPayCountriesUseCase: GetTangemPayCountriesUseCase,
    private val getTangemPayRegionsUseCase: GetTangemPayRegionsUseCase,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawComponent.Params>()

    val draft: StateFlow<BankWithdrawDraft>
        field = MutableStateFlow(BankWithdrawDraft(entry = params.entry))

    /** Every saved account of the wallet: the recipient forms suggest from it while an account number is typed. */
    val savedAccounts: StateFlow<List<BankAccount>>
        field = MutableStateFlow(emptyList<BankAccount>())

    init {
        loadSavedAccounts()
        prefetchResidency()
    }

    fun onRecipientSelected(account: BankAccount) {
        draft.update { it.withSavedRecipient(account) }
    }

    /**
     * A suggested account replaces the recipient being typed. The typed forms leave the stack with it, and their
     * values and picked names go too: the preview would otherwise show them next to the saved account's details.
     */
    fun onSavedRecipientSelected(account: BankAccount) {
        draft.update { it.withSavedRecipient(account).copy(newRecipient = NewRecipientDraft.forEntry(it.entry)) }
    }

    fun onTransferTypeSelected(isThirdParty: Boolean) {
        draft.update { it.copy(newRecipient = it.newRecipient.copy(isThirdParty = isThirdParty)) }
    }

    fun onAccountTypeSelected(accountType: BankAccountType) {
        draft.update { it.copy(newRecipient = it.newRecipient.copy(accountType = accountType)) }
    }

    fun onDetailsFilled(result: BankWithdrawFormResult) {
        draft.update { current ->
            current.copy(
                newRecipient = current.newRecipient.copy(
                    fields = current.newRecipient.fields + result.values,
                    displayNames = current.newRecipient.displayNames + result.displayNames,
                ),
            )
        }
    }

    fun onAmountConfirmed(result: BankWithdrawAmountResult) {
        draft.update {
            it.copy(amount = result.amount, source = result.source, destination = result.destination)
        }
    }

    fun onOrderCreated(result: BankOfframpOrderResult) {
        draft.update { it.copy(order = result) }
    }

    /** The transfer is on its way, so the balance is refetched. */
    fun onOrderSigned() {
        analytics.send(TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = draft.value.destination?.rail))
        modelScope.launch {
            paymentAccountStatusFetcher(params.userWalletId).onLeft { error ->
                TangemLogger.withTag(TAG).e("Failed to refresh the payment account after a bank withdraw: $error")
            }
        }
    }

    /** The wizard's address steps open two pickers; warming them here keeps the first tap instant. */
    private fun prefetchResidency() {
        if (TangemPayBankWithdrawInnerRoute.RecipientDetails !in BankWithdrawFlow.steps(params.entry)) return
        modelScope.launch {
            getTangemPayCountriesUseCase()
            getTangemPayRegionsUseCase(BankWithdrawFormFields.US_COUNTRY_CODE)
        }
    }

    /** One request per wizard: the suggestions and the recipient of a [BankWithdrawEntry.Saved] entry share it. */
    private fun loadSavedAccounts() {
        modelScope.launch {
            getBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = { error -> TangemLogger.withTag(TAG).e("Failed to load saved bank accounts: $error") },
                ifRight = { accounts ->
                    savedAccounts.value = accounts
                    selectEntryRecipient(accounts)
                },
            )
        }
    }

    private fun selectEntryRecipient(accounts: List<BankAccount>) {
        val entry = params.entry as? BankWithdrawEntry.Saved ?: return
        accounts.firstOrNull { it.id == entry.bankAccountId }?.let(::onRecipientSelected)
    }

    private fun BankWithdrawDraft.withSavedRecipient(account: BankAccount): BankWithdrawDraft = copy(
        recipient = account,
        destination = destination?.let { current ->
            params.options.value.destinations.firstOrNull { it.rail == account.details.rail } ?: current
        },
    )
}

private const val TAG = "TangemPayBankWithdrawModel"