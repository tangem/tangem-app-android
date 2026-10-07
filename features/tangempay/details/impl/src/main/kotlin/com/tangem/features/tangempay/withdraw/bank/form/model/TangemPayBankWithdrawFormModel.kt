package com.tangem.features.tangempay.withdraw.bank.form.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.form.TangemPayBankWithdrawFormComponent
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUM
import com.tangem.features.tangempay.withdraw.bank.form.state.TangemPayBankWithdrawFormUMFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
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
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<Params>()
    private val form = params.form

    private val screen = when (form.step) {
        TangemPayBankWithdrawInnerRoute.BankDetails,
        TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.BankDetails),
        -> FormScreen.BankDetails
        else -> FormScreen.RecipientDetails
    }

    private val umFactory = TangemPayBankWithdrawFormUMFactory(
        fields = form.fields,
        step = form.step,
        callbacks = TangemPayBankWithdrawFormUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onCloseClick = form.onCloseFlow,
            onValueChange = ::onValueChange,
            onFocusChange = ::onFocusChange,
            onSelectorClick = ::onSelectorClick,
            onSuggestionClick = form.onSavedRecipientSelected,
            onContinueClick = ::onContinueClick,
        ),
    )

    private val state = MutableStateFlow(FormState())

    private val fieldsReportedAsFilled = mutableSetOf<BankWithdrawFormField>()

    val uiState: StateFlow<TangemPayBankWithdrawFormUM>
        field = MutableStateFlow(
            umFactory.create(
                draft = form.draft.value,
                state = state.value,
                accounts = form.savedAccounts.value,
            ),
        )

    init {
        analytics.send(screenOpenedEvent())
        combine(form.draft, state, form.savedAccounts) { draft, formState, accounts ->
            umFactory.create(draft = draft, state = formState, accounts = accounts)
        }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    fun onCountrySelected(field: BankWithdrawFormField, country: TangemPayCountry, region: TangemPayRegion?) {
        val regionField = when (field) {
            BankWithdrawFormField.Country -> BankWithdrawFormField.Region
            else -> null
        }
        val regionCode = region?.code?.subdivisionCode(country.code).orEmpty()
        val staleFields = BankWithdrawFormValidator.dependentFieldsOf(field) + field
        val isCountryChanged = currentValues()[field] != country.code
        state.update { current ->
            current.copy(
                edits = current.edits + (field to country.code) +
                    listOfNotNull(regionField?.to(regionCode)),
                displayNameEdits = current.displayNameEdits + (field to country.name) +
                    listOfNotNull(regionField?.to(region?.name.orEmpty())),
                errors = current.errors - staleFields.toSet(),
            )
        }
        // A new country wipes the region, so the region the user picks next is a fill of its own.
        if (isCountryChanged && regionField != null) {
            fieldsReportedAsFilled -= regionField
        }
        reportFieldFilled(field)
        if (regionField != null && region != null) {
            reportFieldFilled(regionField)
        }
    }

    fun onRegionSelected(field: BankWithdrawFormField, region: TangemPayRegion) {
        val regionCode = region.code.subdivisionCode(countryCodeOf(field))
        state.update { current ->
            current.copy(
                edits = current.edits + (field to regionCode),
                displayNameEdits = current.displayNameEdits + (field to region.name),
                errors = current.errors - field,
            )
        }
        reportFieldFilled(field)
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
        // Typing replaces whatever was picked, so the name is blanked instead of dropped: a dropped key would
        // fall through to the draft and resurface the previous pick's name.
        state.update { current ->
            current.copy(
                edits = current.edits + (field to value),
                displayNameEdits = current.displayNameEdits + (field to ""),
                errors = current.errors - staleFields,
            )
        }
    }

    private fun onFocusChange(field: BankWithdrawFormField, isFocused: Boolean) {
        if (isFocused) {
            state.update { it.copy(focusedField = field) }
            return
        }
        // A blur can arrive after the next field has already reported focus, so only its own field is cleared.
        state.update { if (it.focusedField == field) it.copy(focusedField = null) else it }
        reportFieldFilled(field)
        val error = BankWithdrawFormValidator.validate(field = field, values = currentValues())
        // An empty field is only reported on Continue, so tabbing through the form stays quiet.
        if (error == null || error == BankWithdrawFieldError.Required) return
        state.update { it.copy(errors = it.errors + (field to error)) }
    }

    private fun onSelectorClick(field: BankWithdrawFormField) {
        val values = currentValues()
        val config = when (field) {
            BankWithdrawFormField.Country -> {
                CountrySelectorConfig.Country(
                    field = field,
                    selectedCountryCode = values.pickedCode(BankWithdrawFormField.Country),
                    selectedRegionCode = values.pickedCode(BankWithdrawFormField.Region),
                )
            }
            BankWithdrawFormField.Region -> {
                regionConfig(field, values = values, countryField = BankWithdrawFormField.Country)
            }
            BankWithdrawFormField.BankRegion -> {
                CountrySelectorConfig.Region(
                    field = field,
                    countryCode = BankWithdrawFormFields.US_COUNTRY_CODE,
                    selectedRegionCode = values.pickedCode(field),
                )
            }
            else -> null
        }
        config?.let { params.onOpenSelector(it) }
    }

    private fun onContinueClick() {
        // The field being typed in never blurs before Continue, so its fill is reported here instead.
        state.value.focusedField?.let(::reportFieldFilled)
        val values = currentValues()
        val errors = BankWithdrawFormValidator.validateAll(fields = form.fields, values = values)
        if (errors.isNotEmpty()) {
            state.update { it.copy(errors = errors, focusedField = null) }
        } else {
            analytics.send(continueClickedEvent())
            form.onContinue(result())
            state.update { it.copy(edits = emptyMap(), displayNameEdits = emptyMap(), errors = emptyMap()) }
        }
    }

    /** This screen's fields, the user's local edits layered over the draft's current values. */
    private fun currentValues(): Map<BankWithdrawFormField, String> = state.value.values(form.fields, form.draft.value)

    private fun result(): BankWithdrawFormResult = BankWithdrawFormResult(
        values = currentValues().mapValues { (_, value) -> value.trim() },
        displayNames = state.value.displayNames(form.fields, form.draft.value),
    )

    private fun regionConfig(
        field: BankWithdrawFormField,
        values: Map<BankWithdrawFormField, String>,
        countryField: BankWithdrawFormField,
    ): CountrySelectorConfig? {
        return values.pickedCode(countryField)?.let { countryCode ->
            CountrySelectorConfig.Region(
                field = field,
                countryCode = countryCode,
                selectedRegionCode = values.pickedCode(field),
            )
        }
    }

    /** An untouched field holds an empty string, which the picker reads as "nothing picked yet". */
    private fun Map<BankWithdrawFormField, String>.pickedCode(field: BankWithdrawFormField): String? =
        get(field)?.takeIf(String::isNotEmpty)

    /** Bank rails are US-only, so a bank state is always American; a recipient's state follows the country picked. */
    private fun countryCodeOf(field: BankWithdrawFormField): String = when (field) {
        BankWithdrawFormField.BankRegion -> BankWithdrawFormFields.US_COUNTRY_CODE
        else -> state.value.value(BankWithdrawFormField.Country, form.draft.value)
    }

    /** A field counts as filled the first time it holds a value, so a re-entered answer is reported only once. */
    private fun reportFieldFilled(field: BankWithdrawFormField) {
        if (field in fieldsReportedAsFilled || currentValues()[field].orEmpty().isBlank()) return
        fieldsReportedAsFilled += field
        analytics.send(fieldFilledEvent(field))
    }

    private fun screenOpenedEvent(): TangemPayAnalyticsEvents = when (screen) {
        FormScreen.RecipientDetails -> TangemPayAnalyticsEvents.Offramp.RecipientDetailsScreenOpened()
        FormScreen.BankDetails -> TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsScreenOpened()
    }

    private fun fieldFilledEvent(field: BankWithdrawFormField): TangemPayAnalyticsEvents {
        val name = field.toAnalyticsName()
        return when (screen) {
            FormScreen.RecipientDetails -> TangemPayAnalyticsEvents.Offramp.RecipientDetailsFieldFilled(name)
            FormScreen.BankDetails -> TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsFieldFilled(name)
        }
    }

    private fun continueClickedEvent(): TangemPayAnalyticsEvents = when (screen) {
        FormScreen.RecipientDetails -> TangemPayAnalyticsEvents.Offramp.RecipientDetailsContinueClicked()
        FormScreen.BankDetails -> TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsContinueClicked()
    }

    /** The two form steps this model serves, each reporting its own family of events. */
    private enum class FormScreen { RecipientDetails, BankDetails }

    data class Params(
        val form: TangemPayBankWithdrawFormComponent.Params,
        val onOpenSelector: (CountrySelectorConfig) -> Unit,
    )
}