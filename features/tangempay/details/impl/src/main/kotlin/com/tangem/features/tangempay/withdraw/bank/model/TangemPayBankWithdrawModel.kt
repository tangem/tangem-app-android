package com.tangem.features.tangempay.withdraw.bank.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.TangemPayBankWithdrawComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawComponent.Params>()

    val draft: StateFlow<BankWithdrawDraft>
        field = MutableStateFlow(BankWithdrawDraft(entry = params.entry))

    init {
        loadSavedRecipient()
    }

    fun onRecipientSelected(account: BankAccount) {
        draft.update { it.copy(recipient = account) }
    }

    fun onAmountConfirmed(result: BankWithdrawAmountResult) {
        draft.update {
            it.copy(amount = result.amount, source = result.source, destination = result.destination)
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