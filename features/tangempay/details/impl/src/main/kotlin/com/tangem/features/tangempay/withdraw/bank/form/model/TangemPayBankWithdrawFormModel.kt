package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
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

    private val params = paramsContainer.require<Params>()
    private val form = params.form

    private val umFactory = TangemPayBankWithdrawFormUMFactory(
        fields = form.fields,
        step = form.step,
        callbacks = TangemPayBankWithdrawFormUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onValueChange = ::onValueChange,
            onFocusChange = ::onFocusChange,
            onSelectorClick = ::onSelectorClick,
            onContinueClick = ::onContinueClick,
        ),
    )

    private val state = MutableStateFlow(
        FormState(
            values = form.fields.associateWith { field -> form.draft.value.newRecipient.fields[field].orEmpty() },
        ),
    )

    val uiState: StateFlow<TangemPayBankWithdrawFormUM>
        field = MutableStateFlow(umFactory.create(draft = form.draft.value, state = state.value))

    init {
        combine(form.draft, state) { draft, formState -> umFactory.create(draft = draft, state = formState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    fun onCountrySelected(field: BankWithdrawFormField, country: TangemPayCountry, region: TangemPayRegion?) {
        val regionField = when (field) {
            BankWithdrawFormField.Country -> BankWithdrawFormField.Region
            BankWithdrawFormField.BankCountry -> BankWithdrawFormField.BankRegion
            else -> null
        }
        val staleFields = BankWithdrawFormValidator.dependentFieldsOf(field) + field
        state.update { current ->
            current.copy(
                values = current.values + (field to country.code) +
                    listOfNotNull(regionField?.to(region?.code.orEmpty())),
                errors = current.errors - staleFields.toSet(),
            )
        }
    }

    fun onRegionSelected(field: BankWithdrawFormField, region: TangemPayRegion) {
        state.update { current ->
            current.copy(values = current.values + (field to region.code), errors = current.errors - field)
        }
    }

    private fun onBackClick() {
        form.onBack(result())
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

    private fun onSelectorClick(field: BankWithdrawFormField) {
        val config = when (field) {
            BankWithdrawFormField.Country, BankWithdrawFormField.BankCountry -> {
                CountrySelectorConfig.Country(field)
            }
            BankWithdrawFormField.Region -> {
                regionConfig(field, countryField = BankWithdrawFormField.Country)
            }
            BankWithdrawFormField.BankRegion -> {
                regionConfig(field, countryField = BankWithdrawFormField.BankCountry)
            }
            else -> null
        }
        config?.let { params.onOpenSelector(it) }
    }

    private fun onContinueClick() {
        val current = state.value
        val visibleFields = BankWithdrawFormFields.visible(fields = form.fields, values = current.values)
        val errors = BankWithdrawFormValidator.validateAll(fields = visibleFields, values = current.values)
        if (errors.isNotEmpty()) {
            state.update { it.copy(errors = errors) }
        } else {
            state.update { it.copy(errors = emptyMap()) }
            form.onContinue(result())
        }
    }

    private fun result(): Map<BankWithdrawFormField, String> =
        state.value.values.mapValues { (_, value) -> value.trim() }

    private fun regionConfig(
        field: BankWithdrawFormField,
        countryField: BankWithdrawFormField,
    ): CountrySelectorConfig? {
        return state.value.values[countryField]
            ?.takeIf(String::isNotEmpty)
            ?.let { CountrySelectorConfig.Region(field = field, countryCode = it) }
    }

    data class Params(
        val form: TangemPayBankWithdrawFormComponent.Params,
        val onOpenSelector: (CountrySelectorConfig) -> Unit,
    )
}