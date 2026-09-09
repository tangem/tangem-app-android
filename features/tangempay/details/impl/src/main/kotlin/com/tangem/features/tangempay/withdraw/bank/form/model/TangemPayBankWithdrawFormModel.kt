package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.tangempay.withdraw.bank.form.TangemPayBankWithdrawFormComponent
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawFormModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawFormComponent.Params>()

    private val umFactory = TangemPayBankWithdrawFormUMFactory(
        fields = params.fields,
        step = params.step,
        callbacks = TangemPayBankWithdrawFormUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onValueChange = ::onValueChange,
            onFocusChange = ::onFocusChange,
            onContinueClick = ::onContinueClick,
        ),
    )

    private val state = MutableStateFlow(
        FormState(
            values = params.fields.associateWith { field -> params.draft.value.newRecipient.fields[field].orEmpty() },
        ),
    )

    val uiState: StateFlow<TangemPayBankWithdrawFormUM>
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, state = state.value))

    init {
        combine(params.draft, state) { draft, formState -> umFactory.create(draft = draft, state = formState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun onBackClick() {
        params.onBack(state.value.values.mapValues { (_, value) -> value.trim() })
        router.pop()
    }

    private fun onValueChange(field: BankWithdrawFormField, value: String) {
        val staleFields = BankWithdrawFormValidator.dependentFieldsOf(field) + field
        state.update { it.copy(values = it.values + (field to value), errors = it.errors - staleFields) }
    }

    private fun onFocusChange(field: BankWithdrawFormField, isFocused: Boolean) {
        if (isFocused) return
        val error = BankWithdrawFormValidator.validate(field = field, values = state.value.values)
        // An empty field is only reported on Continue, so tabbing through the form stays quiet.
        if (error == null || error == BankWithdrawFieldError.Required) return
        state.update { it.copy(errors = it.errors + (field to error)) }
    }

    private fun onContinueClick() {
        val values = state.value.values
        val errors = BankWithdrawFormValidator.validateAll(fields = params.fields, values = values)
        if (errors.isNotEmpty()) {
            state.update { it.copy(errors = errors) }
        } else {
            state.update { it.copy(errors = emptyMap()) }
            params.onContinue(values.mapValues { (_, value) -> value.trim() })
        }
    }
}