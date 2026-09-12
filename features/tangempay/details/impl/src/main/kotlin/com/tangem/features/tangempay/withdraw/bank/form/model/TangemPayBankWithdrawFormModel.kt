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
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
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

    private val state = MutableStateFlow(FormState())

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
                edits = current.edits + (field to country.code) +
                    listOfNotNull(regionField?.to(region?.code.orEmpty())),
                errors = current.errors - staleFields.toSet(),
            )
        }
    }

    fun onRegionSelected(field: BankWithdrawFormField, region: TangemPayRegion) {
        state.update { current ->
            current.copy(edits = current.edits + (field to region.code), errors = current.errors - field)
        }
    }

    private fun onBackClick() {
        // Edit mode means "cancel": Continue validates before writing to the draft, but Back does not, so
        // discarding here avoids committing a half-typed value or a blanked required field.
        if (form.step !is TangemPayBankWithdrawInnerRoute.Edit) {
            form.onBack(result())
        }
        router.pop()
    }

    private fun onValueChange(field: BankWithdrawFormField, value: String) {
        val staleFields = BankWithdrawFormValidator.dependentFieldsOf(field) + field
        state.update { it.copy(edits = it.edits + (field to value), errors = it.errors - staleFields) }
    }

    private fun onFocusChange(field: BankWithdrawFormField, isFocused: Boolean) {
        if (isFocused) return
        val error = BankWithdrawFormValidator.validate(field = field, values = currentValues())
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
        val values = currentValues()
        val visibleFields = BankWithdrawFormFields.visible(fields = form.fields, values = values)
        val errors = BankWithdrawFormValidator.validateAll(fields = visibleFields, values = values)
        if (errors.isNotEmpty()) {
            state.update { it.copy(errors = errors) }
        } else {
            form.onContinue(values.mapValues { (_, value) -> value.trim() })
            state.update { it.copy(edits = emptyMap(), errors = emptyMap()) }
        }
    }

    /** This screen's fields, the user's local edits layered over the draft's current values. */
    private fun currentValues(): Map<BankWithdrawFormField, String> = state.value.values(form.fields, form.draft.value)

    private fun result(): Map<BankWithdrawFormField, String> = currentValues().mapValues { (_, value) -> value.trim() }

    private fun regionConfig(
        field: BankWithdrawFormField,
        countryField: BankWithdrawFormField,
    ): CountrySelectorConfig? {
        return currentValues()[countryField]
            ?.takeIf(String::isNotEmpty)
            ?.let { CountrySelectorConfig.Region(field = field, countryCode = it) }
    }

    data class Params(
        val form: TangemPayBankWithdrawFormComponent.Params,
        val onOpenSelector: (CountrySelectorConfig) -> Unit,
    )
}