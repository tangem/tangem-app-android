package com.tangem.features.addressbook.addaddress.model

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.addaddress.AddAddressComponent
import com.tangem.features.addressbook.addaddress.contract.AddAddressUM
import com.tangem.features.addressbook.addaddress.contract.AddressFieldUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.collections.map

@OptIn(FlowPreview::class)
@ModelScoped
internal class AddAddressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    multiAccountListSupplier: MultiAccountListSupplier,
    private val clipboardManager: ClipboardManager,
) : Model() {

    private val params: AddAddressComponent.Params = paramsContainer.require()

    val state: StateFlow<AddAddressUM>
        field = MutableStateFlow(getInitialState())

    private val availableCoins: StateFlow<List<CryptoCurrency.Coin>> = multiAccountListSupplier()
        .map { accountLists ->
            accountLists
                .flatMap { it.accounts }
                .filterIsInstance<Account.CryptoPortfolio>()
                .flatMap { it.cryptoCurrencies }
                .filterIsInstance<CryptoCurrency.Coin>()
                .distinctBy { it.network.id }
        }
        .stateIn(modelScope, SharingStarted.Eagerly, emptyList())

    private val addressInput = state
        .map { it.addressField.value }
        .distinctUntilChanged()
        .debounce(ADD_ADDRESS_DEBOUNCE)

    init {
        subscribeToAddressInput()
    }

    private fun onAddressChange(value: String, isPasted: Boolean = false) {
        state.update { oldState ->
            oldState.copy(
                addressField = oldState.addressField.copy(
                    value = value,
                    isValuePasted = isPasted,
                    isError = false,
                    error = null,
                ),
                chosenNetworkState = AddAddressUM.ChosenNetworkState.Loading,
            )
        }
    }

    private fun subscribeToAddressInput() {
        combine(addressInput, availableCoins) { input, coins ->
            getUniqueNetworks(input, coins)
        }
            .onEach { availableNetworks ->
                state.update { oldState ->
                    oldState.copy(
                        availableNetworks = availableNetworks,
                        chosenNetworkState = createChosenNetworkState(availableNetworks),
                    )
                }
            }
            .launchIn(modelScope)
    }

    private fun createChosenNetworkState(availableNetworks: ImmutableList<Network>): AddAddressUM.ChosenNetworkState {
        return if (availableNetworks.isEmpty()) {
            AddAddressUM.ChosenNetworkState.Empty
        } else {
            AddAddressUM.ChosenNetworkState.Result(
                networkUMList = availableNetworks
                    .map { network ->
                        AddAddressUM.ChosenNetworkState.Result.NetworkUM(
                            networkName = network.name,
                            iconResId = network.iconResId,
                        )
                    }
                    .toImmutableList(),
            )
        }
    }

    private fun getUniqueNetworks(input: String, coins: List<CryptoCurrency.Coin>): ImmutableList<Network> {
        return coins
            .filter { it.network.toBlockchain().validateAddress(input) }
            .map { it.network }
            .toImmutableList()
    }

    private fun onPaste() {
        onAddressChange(value = clipboardManager.getText().orEmpty(), isPasted = true)
    }

    private fun validateAndConfirm() {
        // TODO([REDACTED_TASK_KEY]): validate the address and invoke params.onConfirm
    }

    private fun getInitialState(): AddAddressUM = AddAddressUM(
        addressField = AddressFieldUM(
            value = "",
            placeholder = resourceReference(R.string.common_address),
            label = resourceReference(R.string.address_book_enter_address),
            isError = false,
            error = null,
            isValuePasted = false,
        ),
        availableNetworks = persistentListOf(),
        buttonUM = TangemButtonUM(
            text = TextReference.Res(R.string.address_book_add_address),
            type = TangemButtonType.Primary,
            isEnabled = false,
            onClick = ::validateAndConfirm,
        ),
        chosenNetworkState = AddAddressUM.ChosenNetworkState.Empty,
        onAddressChange = { onAddressChange(value = it) },
        onAddressClear = { onAddressChange("") },
        onPasteClick = ::onPaste,
        onQrClick = { /* [REDACTED_TODO_COMMENT] */ },
        onBackClick = params.onBackClick,
    )

    companion object {
        private const val ADD_ADDRESS_DEBOUNCE = 500L
    }
}