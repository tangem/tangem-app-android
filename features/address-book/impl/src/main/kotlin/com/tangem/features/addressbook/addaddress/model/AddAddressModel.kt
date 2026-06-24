package com.tangem.features.addressbook.addaddress.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.addaddress.state.AddAddressStateController
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddAddressInitialStateTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressInputTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressValidationTransformer
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@ModelScoped
internal class AddAddressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    multiAccountListSupplier: MultiAccountListSupplier,
    private val clipboardManager: ClipboardManager,
    private val stateController: AddAddressStateController,
) : Model() {

    private val params: DefaultAddAddressComponent.Params = paramsContainer.require()

    val state: StateFlow<AddAddressUM> get() = stateController.uiState

    private val availableCoins: StateFlow<List<CryptoCurrency.Coin>> = multiAccountListSupplier()
        .map { accountLists ->
            accountLists
                .flatMap { it.flattenCurrencies() }
                .filterIsInstance<CryptoCurrency.Coin>()
                .distinctBy { it.network.id }
        }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, emptyList())

    private val addressInput = state
        .map { it.addressField.value }
        .distinctUntilChanged()
        .debounce(ADD_ADDRESS_DEBOUNCE)

    init {
        updateInitialState()
        subscribeToAddressValidation()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateAddAddressInitialStateTransformer(
                onAddressChange = { onAddressChange(value = it) },
                onAddressClear = { onAddressChange("") },
                onPasteClick = ::onPaste,
                onQrClick = { /* [REDACTED_TODO_COMMENT] */ },
                onBackClick = params.onBackClick,
                onConfirmClick = ::validateAndConfirm,
            ),
        )
    }

    private fun onAddressChange(value: String) {
        stateController.update(UpdateAddressInputTransformer(value = value))
    }

    private fun subscribeToAddressValidation() {
        combine(addressInput, availableCoins) { input, coins ->
            UpdateAddressValidationTransformer(address = input, coins = coins)
        }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun onPaste() {
        onAddressChange(value = clipboardManager.getText().orEmpty())
    }

    private fun validateAndConfirm() {
        // TODO Address book ([REDACTED_TASK_KEY]): navigate to the network-selection with the address and its matching networks.
    }

    companion object {
        private const val ADD_ADDRESS_DEBOUNCE = 500L
    }
}