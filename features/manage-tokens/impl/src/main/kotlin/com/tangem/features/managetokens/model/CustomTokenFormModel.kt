package com.tangem.features.managetokens.model

import androidx.compose.ui.res.stringResource
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.SimpleOkDialog
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ContentMessage
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.CustomCurrencyValidator
import com.tangem.features.managetokens.utils.mapper.mapToDomainModel
import com.tangem.features.managetokens.utils.ui.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
internal class CustomTokenFormModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val customCurrencyValidator: CustomCurrencyValidator,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val messageSender: UiMessageSender,
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
        } else {
            modelScope.launch {
                customCurrencyValidator.createCoin(
                    userWalletId = params.userWalletId,
                    networkId = params.network.id,
                    derivationPath = getDerivationPath(),
                )
            }
        }
    }

    private fun getInitialState(): CustomTokenFormUM {
        return CustomTokenFormUM(
            networkName = ClickableFieldUM(
                label = resourceReference(R.string.custom_token_network_input_title),
                value = params.network.name,
                onClick = ::selectNetwork,
            ),
            tokenForm = if (params.network.canHandleTokens) {
                getInitialTokenForm()
            } else {
                null
            },
            derivationPath = ClickableFieldUM(
                label = resourceReference(R.string.custom_token_derivation_path),
                value = if (params.derivationPath == null || params.derivationPath.id == params.network.id) {
                    resourceReference(R.string.custom_token_derivation_path_default)
                } else {
                    params.derivationPath.networkName
                },
                onClick = ::selectDerivationPath,
            ),
            saveToken = ::addCurrency,
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeTokenFormUpdates() {
        state
            .sample(periodMillis = 1_000)
            .transform {
                val values = it.tokenForm?.mapToDomainModel()

                if (values != null) {
                    emit(values)
                }
            }
            .distinctUntilChanged()
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
                is CustomCurrencyValidator.State.NotStarted -> Unit
                is CustomCurrencyValidator.State.SearchingToken -> updateStateWithSearching()
                is CustomCurrencyValidator.State.UnexpectedException -> showErrorDialog()
                is CustomCurrencyValidator.State.FormValidationException -> updateStateWithExceptions(
                    exceptions = validatorState.exceptions,
                )
                is CustomCurrencyValidator.State.TokenNotFound -> updateStateWithNotFoundNotification()
                is CustomCurrencyValidator.State.Validated -> {
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

    private fun updateStateWithCurrency(
        currency: CryptoCurrency,
        fillForm: Boolean,
        isAlreadyAdded: Boolean,
        isCustom: Boolean,
    ) {
        state.update { state ->
            var updatedState = state
                .updateWithProgress(
                    showProgress = false,
                    canAddToken = !isAlreadyAdded,
                    clearNotifications = true,
                    clearFieldErrors = true,
                    disableSecondaryFields = !isCustom,
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

    private fun updateStateWithSearching() {
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
        val dialog = ContentMessage { onDismiss ->
            SimpleOkDialog(
                message = stringResource(R.string.common_unknown_error),
                onDismissDialog = onDismiss,
            )
        }

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

    private fun getInitialTokenForm(): CustomTokenFormUM.TokenFormUM {
        val formValues = params.formValues

        val form = CustomTokenFormUM.TokenFormUM(
            contractAddress = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_contract_address_input_title),
                placeholder = stringReference(CONTRACT_ADDRESS_PLACEHOLDER),
                onValueChange = ::updateContractAddress,
            ),
            name = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_name_input_title),
                placeholder = resourceReference(R.string.custom_token_name_input_placeholder),
                onValueChange = ::updateTokenName,
            ),
            symbol = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_token_symbol_input_title),
                placeholder = resourceReference(R.string.custom_token_token_symbol_input_placeholder),
                onValueChange = ::updateTokenSymbol,
            ),
            decimals = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_decimals_input_title),
                placeholder = stringReference(DECIMALS_PLACEHOLDER),
                onValueChange = ::updateDecimals,
            ),
        )

        return formValues.fillValues(form)
    }

    private fun getDerivationPath(): Network.DerivationPath {
        return params.derivationPath?.value ?: params.network.derivationPath
    }

    private fun updateContractAddress(value: String) {
        state.update { state ->
            state.updateTokenForm {
                copy(contractAddress = contractAddress.updateValue(value))
            }
        }
    }

    private fun updateTokenName(value: String) {
        state.update { state ->
            state.updateTokenForm {
                copy(name = name.updateValue(value))
            }
        }
    }

    private fun updateTokenSymbol(value: String) {
        state.update { state ->
            state.updateTokenForm {
                copy(symbol = symbol.updateValue(value))
            }
        }
    }

    private fun updateDecimals(value: String) {
        state.update { state ->
            state.updateTokenForm {
                copy(decimals = decimals.updateValue(value))
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

    private companion object {
        const val CONTRACT_ADDRESS_PLACEHOLDER = "0x000000000000000000000000000..."
        const val DECIMALS_PLACEHOLDER = "0"
    }
}