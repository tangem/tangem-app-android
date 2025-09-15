package com.tangem.features.managetokens.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.HasMissedDerivationsUseCase
import com.tangem.features.managetokens.analytics.CustomTokenAnalyticsEvent
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM.Field
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.CustomCurrencyFormBuilder
import com.tangem.features.managetokens.utils.CustomCurrencyValidator
import com.tangem.features.managetokens.utils.mapper.mapToDomainModel
import com.tangem.features.managetokens.utils.ui.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.mutate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// TODO: Divide to sub-components: [REDACTED_JIRA]
@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class CustomTokenFormModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val customCurrencyValidator: CustomCurrencyValidator,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val messageSender: UiMessageSender,
    private val customTokenFormManager: CustomCurrencyFormBuilder,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: CustomTokenFormComponent.Params = paramsContainer.require()
    private var createdCurrency: CryptoCurrency? = null

    val state: MutableStateFlow<CustomTokenFormUM> = MutableStateFlow(
        value = getInitialState(),
    )

    init {
        observeValidatorUpdates()

        if (params.network.canHandleTokens) {
            observeTokenFormUpdates()
        }

        validatePrefilledForm()
    }

    private fun getInitialState(): CustomTokenFormUM {
        return CustomTokenFormUM(
            networkName = ClickableFieldUM(
                label = resourceReference(R.string.custom_token_network_input_title),
                value = stringReference(params.network.name),
                onClick = ::selectNetwork,
            ),
            tokenForm = if (params.network.canHandleTokens) {
                customTokenFormManager.buildForm(
                    updateFormFieldValue = ::updateFormFieldValue,
                    updateFormFieldFocus = ::updateFormFieldFocus,
                )
            } else {
                null
            },
            derivationPath = getDerivationPathFormIfSupported(),
            saveToken = ::addCurrency,
        )
    }

    private fun getDerivationPathFormIfSupported(): ClickableFieldUM? {
        // If network doesn't support derivation path, return null
        if (params.network.derivationPath is Network.DerivationPath.None) return null

        return ClickableFieldUM(
            label = resourceReference(R.string.custom_token_derivation_path),
            value = if (params.derivationPath == null || params.derivationPath.isDefault) {
                resourceReference(R.string.custom_token_derivation_path_default)
            } else {
                stringReference(params.derivationPath.name)
            },
            onClick = ::selectDerivationPath,
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeTokenFormUpdates() {
        state
            .transform { state ->
                val form = state.tokenForm

                if (form != null && !form.wasFilled) {
                    emit(form.mapToDomainModel())
                }
            }
            .distinctUntilChanged()
            .sample(periodMillis = 1_000)
            .drop(count = 1) // Skip initial state
            .onEach { formValues ->
                customCurrencyValidator.validateForm(
                    userWalletId = params.userWalletId,
                    networkId = params.network.id,
                    derivationPath = getDerivationPath(),
                    formValues = formValues,
                )
            }
            .launchIn(modelScope)
    }

    private fun observeValidatorUpdates() = modelScope.launch {
        customCurrencyValidator.consumeUpdates { validatorState ->
            createdCurrency = null

            when (validatorState) {
                is CustomCurrencyValidator.Status.NotStarted,
                is CustomCurrencyValidator.Status.Validating,
                -> Unit
                is CustomCurrencyValidator.Status.SearchingToken -> updateStateWithProgress()
                is CustomCurrencyValidator.Status.UnexpectedException -> showErrorDialog()
                is CustomCurrencyValidator.Status.FormValidationException -> updateStateWithExceptions(
                    exceptions = validatorState.exceptions,
                )
                is CustomCurrencyValidator.Status.TokenNotFound -> updateStateWithNotFoundNotification()
                is CustomCurrencyValidator.Status.Validated -> {
                    createdCurrency = validatorState.currency

                    updateStateWithCurrency(
                        currency = validatorState.currency,
                        fillForm = validatorState.fillForm,
                        isAlreadyAdded = validatorState.isAlreadyAdded,
                        isCustom = validatorState.isCustom,
                    )
                }
            }
        }
    }

    private fun validatePrefilledForm() = modelScope.launch {
        customCurrencyValidator.validateForm(
            userWalletId = params.userWalletId,
            networkId = params.network.id,
            derivationPath = getDerivationPath(),
            formValues = state.value.tokenForm.mapToDomainModel(),
        )
    }

    private fun updateStateWithCurrency(
        currency: CryptoCurrency,
        fillForm: Boolean,
        isAlreadyAdded: Boolean,
        isCustom: Boolean,
    ) = modelScope.launch {
        val needToAddDerivation = hasMissedDerivationsUseCase(
            userWalletId = params.userWalletId,
            networksWithDerivationPath = mapOf(currency.network.backendId to getDerivationPath().value),
        )

        state.update { state ->
            var updatedState = state
                .updateWithProgress(
                    showProgress = false,
                    canAddToken = !isAlreadyAdded,
                    isWasFilled = fillForm,
                    clearNotifications = true,
                    clearFieldErrors = true,
                    disableSecondaryFields = !isCustom,
                    needToAddDerivation = needToAddDerivation,
                )

            if (fillForm) {
                updatedState = updatedState.updateWithCurrency(currency)
            }

            if (isAlreadyAdded) {
                updatedState = updatedState.updateWithCurrencyAlreadyAddedNotification()
            }

            if (isCustom) {
                updatedState = updatedState.updateWithCurrencyNotFoundNotification()
            }

            updatedState
        }
    }

    private fun updateStateWithNotFoundNotification() {
        state.update { state ->
            state
                .updateWithProgress(
                    showProgress = false,
                    canAddToken = false,
                    clearNotifications = true,
                    clearFieldErrors = true,
                )
                .updateWithCurrencyNotFoundNotification()
        }
    }

    private fun updateStateWithProgress() {
        state.update { state ->
            state.updateWithProgress(
                showProgress = true,
                canAddToken = false,
                clearNotifications = false,
                clearFieldErrors = false,
            )
        }
    }

    private fun showErrorDialog() {
        val dialog = DialogMessage(
            message = resourceReference(R.string.common_unknown_error),
        )

        messageSender.send(dialog)
    }

    private fun updateStateWithExceptions(exceptions: List<CustomTokenFormValidationException>) {
        state.update { state ->
            val validatedState = state
                .updateWithProgress(
                    showProgress = false,
                    canAddToken = false,
                    clearNotifications = true,
                    clearFieldErrors = true,
                )

            exceptions.fold(validatedState) { stateAcc, exception ->
                when (exception) {
                    is CustomTokenFormValidationException.ContractAddress -> {
                        stateAcc.updateWithContractAddressException(exception)
                    }
                    is CustomTokenFormValidationException.Decimals -> {
                        stateAcc.updateWithDecimalsException(exception)
                    }
                    is CustomTokenFormValidationException.EmptyName -> {
                        stateAcc // No need to display error
                    }
                    is CustomTokenFormValidationException.EmptySymbol -> {
                        stateAcc // No need to display error
                    }
                    is CustomTokenFormValidationException.DataError -> {
                        Timber.e(exception.cause, "Unable to validate custom currency")
                        stateAcc
                    }
                }
            }
        }
    }

    private fun getDerivationPath(): Network.DerivationPath {
        return params.derivationPath?.value ?: params.network.derivationPath
    }

    private fun updateFormFieldValue(field: Field, value: String) {
        state.update { state ->
            state.updateTokenForm {
                val fieldValue = fields.getValue(field)

                if (!fieldValue.isEnabled) return@updateTokenForm this

                val updatedFieldValue = fieldValue.copy(
                    value = value,
                )
                val updatedFields = fields.mutate {
                    it[field] = updatedFieldValue
                }

                copy(
                    fields = updatedFields,
                    wasFilled = false,
                )
            }
        }
    }

    private fun updateFormFieldFocus(field: Field, isFocused: Boolean) {
        state.update { state ->
            state.updateTokenForm {
                val fieldValue = fields.getValue(field)

                if (!fieldValue.isEnabled) return@updateTokenForm this

                val updatedFieldValue = fieldValue.copy(
                    isFocused = isFocused,
                )
                val updatedFields = fields.mutate {
                    it[field] = updatedFieldValue
                }

                // Checking if a field is out of focus
                if (fieldValue.isFocused && !isFocused && fieldValue.value.isNotEmpty()) {
                    sendFieldAnalyticsEvent(field, fieldValue)
                }

                copy(fields = updatedFields)
            }
        }
    }

    private fun addCurrency() = resource(
        acquire = {
            state.update { state ->
                state.updateWithProgress(showProgress = true)
            }
        },
        release = {
            state.update { state ->
                state.updateWithProgress(showProgress = false)
            }
        },
    ) {
        val currency = createdCurrency
        if (currency == null) {
            Timber.e("Trying to add currency without validation")
            showErrorDialog()
            return@resource
        }

        val event = CustomTokenAnalyticsEvent.CustomTokenWasAdded(
            currencySymbol = currency.symbol,
            derivationPath = currency.network.derivationPath.value.orEmpty(),
            source = params.source,
        )
        analyticsEventHandler.send(event)

        derivePublicKeysUseCase(params.userWalletId, listOf(currency)).getOrElse {
            Timber.e(it, "Failed to derive public keys")
            showErrorDialog()
            return@resource
        }

        addCryptoCurrenciesUseCase(params.userWalletId, currency).getOrElse {
            Timber.e(it, "Failed to add currency")
            showErrorDialog()
            return@resource
        }

        params.onCurrencyAdded()
    }

    private fun selectNetwork() {
        params.onSelectNetworkClick(CustomTokenFormValues(state.value.tokenForm))
    }

    private fun selectDerivationPath() {
        params.onSelectDerivationPathClick(CustomTokenFormValues(state.value.tokenForm))
    }

    private fun sendFieldAnalyticsEvent(field: Field, fieldValue: TextInputFieldUM) {
        val event = when (field) {
            Field.CONTRACT_ADDRESS -> CustomTokenAnalyticsEvent.Address(
                isValid = fieldValue.error == null,
                source = params.source,
            )
            Field.NAME -> CustomTokenAnalyticsEvent.Name(params.source)
            Field.SYMBOL -> CustomTokenAnalyticsEvent.Symbol(params.source)
            Field.DECIMALS -> CustomTokenAnalyticsEvent.Decimals(params.source)
        }

        analyticsEventHandler.send(event)
    }
}