package com.tangem.features.addressbook.addaddress.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.addaddress.state.AddAddressStateController
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddAddressInitialStateTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressInputTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressValidationTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateMemoInputTransformer
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.common.AddressMemoValidator
import com.tangem.features.addressbook.common.SelectNetworksResultHolder
import com.tangem.features.addressbook.common.SupportedNetworksMatcher
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@ModelScoped
internal class AddAddressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val supportedNetworksMatcher: SupportedNetworksMatcher,
    private val memoValidator: AddressMemoValidator,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val clipboardManager: ClipboardManager,
    private val stateController: AddAddressStateController,
    private val selectNetworksResultHolder: SelectNetworksResultHolder,
    private val router: Router,
) : Model() {

    private val params: DefaultAddAddressComponent.Params = paramsContainer.require()

    val state: StateFlow<AddAddressUM> get() = stateController.uiState

    private val validation: StateFlow<AddressValidation> = state
        .map { it.addressField.value }
        .distinctUntilChanged()
        .debounce(ADD_ADDRESS_DEBOUNCE)
        .map { address ->
            AddressValidation(address = address, matchedBlockchains = supportedNetworksMatcher.match(address))
        }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, AddressValidation(address = "", matchedBlockchains = emptyList()))

    /** `true` when a non-blank memo doesn't pass the chosen network's format rules (e.g. XRP destination tag). */
    private val isMemoInvalid = MutableStateFlow(false)

    private val selectedNetworkIds = MutableStateFlow<Set<String>?>(null)

    private val chosenNetworks: StateFlow<ChosenNetworks> = combine(
        validation,
        selectedNetworkIds,
    ) { validation, selected ->
        val matched = validation.matchedBlockchains
        ChosenNetworks(
            address = validation.address,
            matched = matched,
            displayed = displayedNetworks(matched, selected),
            selected = selectedNetworks(matched, selected),
        )
    }
        .flowOn(dispatchers.default)
        .stateIn(
            modelScope,
            SharingStarted.Eagerly,
            ChosenNetworks(address = "", matched = emptyList(), displayed = emptyList(), selected = emptyList()),
        )

    init {
        // Drop any selection left over from a previous AddAddress session before subscribing to it.
        selectNetworksResultHolder.clear()
        updateInitialState()
        subscribeToValidation()
        subscribeToMemoValidation()
        resetSelectionOnAddressChange()
        subscribeToSelectedNetworks()
        subscribeToQrScanResult()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateAddAddressInitialStateTransformer(
                intents = UpdateAddAddressInitialStateTransformer.Intents(
                    onAddressChange = ::onAddressChange,
                    onAddressClear = { onAddressChange("") },
                    onPasteClick = ::onPaste,
                    onQrClick = ::onQrClick,
                    onBackClick = params.onBackClick,
                    onNetworkClick = ::onNetworkClick,
                    onMemoChange = ::onMemoChange,
                    onMemoPasteClick = ::onMemoPaste,
                    onConfirmClick = ::validateAndConfirm,
                ),
            ),
        )
    }

    private fun onAddressChange(value: String) {
        stateController.update(UpdateAddressInputTransformer(value = value))
    }

    private fun onMemoChange(value: String) {
        stateController.update(UpdateMemoInputTransformer(value = value))
    }

    private fun subscribeToValidation() {
        combine(chosenNetworks, isMemoInvalid) { networks, memoInvalid ->
            UpdateAddressValidationTransformer(
                address = networks.address,
                matchedBlockchains = networks.matched,
                displayedBlockchains = networks.displayed,
                selectedBlockchains = networks.selected,
                isMemoInvalid = memoInvalid,
            )
        }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun subscribeToMemoValidation() {
        val memoInput = state.map { it.memoField.value }.distinctUntilChanged().debounce(MEMO_DEBOUNCE)
        combine(memoInput, chosenNetworks) { memo, networks -> memo to networks.extrasBlockchain }
            .mapLatest { (memo, blockchain) ->
                blockchain != null && memo.isNotBlank() && !memoValidator.isValid(blockchain, memo)
            }
            .onEach { isMemoInvalid.value = it }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun resetSelectionOnAddressChange() {
        validation
            .map { it.address }
            .distinctUntilChanged()
            .onEach { selectedNetworkIds.value = null }
            .launchIn(modelScope)
    }

    private fun subscribeToSelectedNetworks() {
        selectNetworksResultHolder.selectedNetworkIds
            .filterNotNull()
            .onEach { ids ->
                selectedNetworkIds.value = ids
                selectNetworksResultHolder.clear()
            }
            .launchIn(modelScope)
    }

    private fun onPaste() {
        onAddressChange(value = clipboardManager.getText().orEmpty())
    }

    private fun onMemoPaste() {
        onMemoChange(value = clipboardManager.getText().orEmpty())
    }

    private fun onQrClick() {
        router.push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.AddressBook))
    }

    private fun subscribeToQrScanResult() {
        listenToQrScanningUseCase(SourceType.ADDRESS_BOOK)
            .getOrElse { emptyFlow() }
            .onEach { onAddressChange(value = normalizeScannedAddress(it)) }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    /**
     * Extracts the bare address from a scanned payment URI like `ethereum:0xADDR@1?amount=1.5`: drops the query
     * (`?…`), the chain suffix (`@…`) and the scheme (`scheme:`). A plain address is returned unchanged.
     */
    private fun normalizeScannedAddress(raw: String): String {
        val withoutQueryAndChain = raw.trim().substringBefore('?').substringBefore('@')
        return withoutQueryAndChain.substringAfter(':', missingDelimiterValue = withoutQueryAndChain)
    }

    private fun onNetworkClick() {
        params.onSelectNetworksClick(
            stateController.uiState.value.addressField.value,
            selectedNetworkIds.value?.toList().orEmpty(),
        )
    }

    private fun validateAndConfirm() {
        val networks = chosenNetworks.value
        if (networks.selected.isEmpty()) return

        val memoField = stateController.uiState.value.memoField
        val memo = memoField.value.trim().takeIf { memoField.isVisible && it.isNotEmpty() }
        params.onConfirm(
            ValidatedAddress(
                address = networks.address,
                networkIds = networks.selected.map { it.toNetworkId() }.toImmutableList(),
                memo = memo,
            ),
        )
    }

    /** What the network block shows: all matched networks until the user narrows them down, then the picked subset. */
    private fun displayedNetworks(matched: List<Blockchain>, selected: Set<String>?): List<Blockchain> {
        if (selected == null) return matched
        return matched.filter { it.toNetworkId() in selected }
    }

    /**
     * What is actually selected for saving. A single matched network is auto-selected (there is nothing to choose and
     * the selection screen can't be opened); otherwise the user must pick explicitly before saving.
     */
    private fun selectedNetworks(matched: List<Blockchain>, selected: Set<String>?): List<Blockchain> {
        if (selected == null) return listOfNotNull(matched.singleOrNull())
        return matched.filter { it.toNetworkId() in selected }
    }

    private data class AddressValidation(
        val address: String,
        val matchedBlockchains: List<Blockchain>,
    )

    private data class ChosenNetworks(
        val address: String,
        val matched: List<Blockchain>,
        val displayed: List<Blockchain>,
        val selected: List<Blockchain>,
    ) {
        /** The first selected network that supports a memo / destination tag, if any. */
        val extrasBlockchain: Blockchain?
            get() = selected.firstOrNull { it.getSupportedTransactionExtras().isTxExtrasSupported() }
    }

    companion object {
        private const val ADD_ADDRESS_DEBOUNCE = 500L
        private const val MEMO_DEBOUNCE = 300L
    }
}