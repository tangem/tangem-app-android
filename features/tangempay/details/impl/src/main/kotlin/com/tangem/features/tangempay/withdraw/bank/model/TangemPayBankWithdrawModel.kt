package com.tangem.features.tangempay.withdraw.bank.model

import androidx.compose.runtime.Stable
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
import com.tangem.domain.pay.usecase.bankofframp.MarkBankAccountUsedUseCase
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
    private val markBankAccountUsedUseCase: MarkBankAccountUsedUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val getTangemPayCountriesUseCase: GetTangemPayCountriesUseCase,
    private val getTangemPayRegionsUseCase: GetTangemPayRegionsUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawComponent.Params>()

    val draft: StateFlow<BankWithdrawDraft>
        field = MutableStateFlow(BankWithdrawDraft(entry = params.entry))

    init {
        loadSavedRecipient()
        prefetchResidency()
    }

    fun onRecipientSelected(account: BankAccount) {
        draft.update { it.copy(recipient = account) }
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

    /** The transfer is on its way: the recipient moves to the top of the recents and the balance is refetched. */
    fun onOrderSigned() {
        val draft = draft.value
        val bankAccountId = draft.order?.bankAccountId ?: draft.recipient?.id
        modelScope.launch {
            bankAccountId?.let {
                markBankAccountUsedUseCase(userWalletId = params.userWalletId, bankAccountId = it)
            }
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

    private fun loadSavedRecipient() {
        val entry = params.entry as? BankWithdrawEntry.Saved ?: return
        modelScope.launch {
            getBankAccountsUseCase(params.userWalletId).fold(
                ifLeft = { error -> TangemLogger.withTag(TAG).e("Failed to load saved bank accounts: $error") },
                ifRight = { accounts ->
                    accounts.firstOrNull { it.id == entry.bankAccountId }?.let { recipient ->
                        draft.update { it.copy(recipient = recipient) }
                    }
                },
            )
        }
    }
}

private const val TAG = "TangemPayBankWithdrawModel"