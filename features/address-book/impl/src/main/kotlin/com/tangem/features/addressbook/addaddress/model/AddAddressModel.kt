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
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.usecase.CheckAddressDuplicateUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.addaddress.state.AddAddressStateController
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddAddressInitialStateTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressInputTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateAddressValidationTransformer
import com.tangem.features.addressbook.addaddress.state.transformers.UpdateMemoInputTransformer
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
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
    private val checkAddressDuplicateUseCase: CheckAddressDuplicateUseCase,
    private val analyticsSender: AddressBookAnalyticsSender,
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
            selected = selectedNetworks(matched, selected),
        )
    }
        .flowOn(dispatchers.default)
        .stateIn(
            modelScope,
            SharingStarted.Eagerly,
            ChosenNetworks(address = "", matched = emptyList(), selected = emptyList()),
        )

    /**
     * Name of the contact that already holds one of the selected `network + address` pairs in the target wallet, or
     * `null` when the pair is free.
     */
    private val duplicateName: StateFlow<String?> = chosenNetworks
        .mapLatest { networks ->
            val walletId = params.walletId ?: return@mapLatest null
            if (networks.selected.isEmpty()) return@mapLatest null
            networks.selected.firstNotNullOfOrNull { blockchain ->
                checkAddressDuplicateUseCase(
                    userWalletId = UserWalletId(walletId),
                    networkId = blockchain.toNetworkId(),
                    address = networks.address,
                    excludeContactId = params.excludeContactId?.let(::ContactId),
                )
            }
        }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, null)

    init {
        // Drop any selection left over from a previous AddAddress session before subscribing to it.
        selectNetworksResultHolder.clear()
        sendInitAnalytics()
        updateInitialState()
        subscribeToValidation()
        subscribeToMemoValidation()
        subscribeToSelectedNetworks()
        subscribeToQrScanResult()
        subscribeToAddressInvalid()
        prefillData()
    }

    private fun prefillData() {
        val prefillAddress = params.prefillAddress ?: return
        onAddressChange(prefillAddress)
        selectedNetworkIds.value = params.prefillNetworkIds.toSet().ifEmpty { null }
        prefillMemoOnceVisible(params.prefillMemo)
    }

    private fun prefillMemoOnceVisible(memo: String?) {
        if (memo.isNullOrEmpty()) return
        state
            .map { it.memoField.isVisible }
            .distinctUntilChanged()
            .filter { isVisible -> isVisible }
            .take(1)
            .onEach { onMemoChange(memo) }
            .launchIn(modelScope)
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
                isEditMode = params.prefillAddress != null,
            ),
        )
    }

    private fun onAddressChange(value: String) {
        stateController.update(UpdateAddressInputTransformer(value = value))
        selectedNetworkIds.value = null
    }

    private fun onMemoChange(value: String) {
        stateController.update(UpdateMemoInputTransformer(value = value))
    }

    private fun subscribeToValidation() {
        combine(chosenNetworks, isMemoInvalid, duplicateName) { networks, memoInvalid, duplicate ->
            UpdateAddressValidationTransformer(
                address = networks.address,
                matchedBlockchains = networks.matched,
                selectedBlockchains = networks.selected,
                isMemoInvalid = memoInvalid,
                duplicateName = duplicate,
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

    private fun subscribeToSelectedNetworks() {
        selectNetworksResultHolder.selectedNetworkIds
            .filterNotNull()
            .onEach { ids ->
                selectedNetworkIds.value = ids
                selectNetworksResultHolder.clear()
            }
            .launchIn(modelScope)
    }

    private fun subscribeToAddressInvalid() {
        val walletId = params.walletId ?: return
        validation
            .map { it.address.isNotBlank() && it.matchedBlockchains.isEmpty() }
            .distinctUntilChanged()
            .filter { isInvalid -> isInvalid }
            .onEach {
                analyticsSender.sendAddressInvalid(
                    walletId = UserWalletId(walletId),
                    contactId = params.excludeContactId.orEmpty(),
                )
            }
            .flowOn(dispatchers.default)
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
            chosenNetworks.value.matched.map { it.toNetworkId() },
            selectedNetworkIds.value?.toList().orEmpty(),
        )
    }

    private fun validateAndConfirm() {
        val networks = chosenNetworks.value
        if (networks.selected.isEmpty()) return
        if (duplicateName.value != null) return

        val memoField = stateController.uiState.value.memoField
        val memo = memoField.value.trim().takeIf { memoField.isVisible && it.isNotEmpty() }
        params.onConfirm(
            ValidatedAddress(
                address = networks.address,
                networkIds = networks.selected.map { it.toNetworkId() }.toImmutableList(),
                memo = memo,
            ),
            // In the edit-address flow this confirmation supersedes the entry the screen was opened for.
            params.prefillAddress,
        )
    }

    /**
     * What is actually selected for saving. A single matched network is auto-selected (there is nothing to choose and
     * the selection screen can't be opened); otherwise the user must pick explicitly before saving.
     */
    private fun selectedNetworks(matched: List<Blockchain>, selected: Set<String>?): List<Blockchain> {
        if (selected == null) return listOfNotNull(matched.singleOrNull())
        return matched.filter { it.toNetworkId() in selected }
    }

    private fun sendInitAnalytics() {
        analyticsSender.sendAddressScreenOpened()
    }

    private data class AddressValidation(
        val address: String,
        val matchedBlockchains: List<Blockchain>,
    )

    private data class ChosenNetworks(
        val address: String,
        val matched: List<Blockchain>,
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