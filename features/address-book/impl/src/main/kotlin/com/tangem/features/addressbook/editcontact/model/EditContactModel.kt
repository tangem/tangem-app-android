package com.tangem.features.addressbook.editcontact.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.usecase.DeleteContactUseCase
import com.tangem.domain.addressbook.usecase.GetContactByIdUseCase
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.addressbook.addressinfo.DefaultAddressInfoComponent
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
import com.tangem.features.addressbook.common.AddressBookResultHolder
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.state.EditContactStateController
import com.tangem.features.addressbook.editcontact.state.transformers.*
import com.tangem.features.addressbook.editcontact.state.transformers.converter.ContactNameErrorConverter
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass", "NamedArguments")
@ModelScoped
internal class EditContactModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: EditContactStateController,
    private val resultHolder: AddressBookResultHolder,
    private val messageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val validateContactNameUseCase: ValidateContactNameUseCase,
    private val saveContactInteractor: SaveContactInteractor,
    private val getContactByIdUseCase: GetContactByIdUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val analyticsSender: AddressBookAnalyticsSender,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : Model() {

    // region State

    private val params: DefaultEditContactComponent.Params = paramsContainer.require()

    /** The contact being edited (null for a new contact). Drives create-vs-update and the delete/discard rules. */
    private val loadedContact = MutableStateFlow<Contact?>(null)

    /** The editor's starting point: the loaded contact once available, otherwise the empty (or predefined) new contact. */
    private val newContactBaseline = EditSnapshot(
        name = "",
        colorName = CryptoPortfolioIcon.Color.entries.first().name,
        addresses = listOfNotNull(params.predefinedAddress).map { it.address to it.networkIds.toSet() },
    )

    /** The in-flight save coroutine — its [Job.isActive] drives both the re-entrancy guard and the button state. */
    private var saveJob: Job? = null

    val state: StateFlow<EditContactUM> get() = stateController.uiState

    val portfolioSelectorNavigation = SlotNavigation<Unit>()
    val addressInfoNavigation = SlotNavigation<String>()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    /** The wallet picked in the selector, if any — single source of the wallet the contact is being saved to. */
    private val pickedWallet: StateFlow<UserWallet?> =
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first }
            .stateIn(modelScope, SharingStarted.Eagerly, null)

    /**
     * The wallet the contact is saved to. For an existing contact it is fixed to the contact's wallet; for a new
     * contact it follows the selector pick and falls back to the app's currently selected wallet.
     */
    private val selectedWallet: StateFlow<UserWallet?> = combine(
        pickedWallet,
        loadedContact,
        userWalletsListRepository.selectedUserWallet,
        userWalletsListRepository.userWallets,
    ) { picked, contact, currentSelected, wallets ->
        when {
            contact != null -> wallets?.firstOrNull { it.walletId == contact.walletId }
            picked != null -> picked
            else -> currentSelected
        }
    }.stateIn(modelScope, SharingStarted.Eagerly, null)

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    // endregion

    init {
        updateInitialState()
        prefillPredefinedAddress()
        subscribeToConfirmedAddresses()
        dismissSelectorOnPick()
        observeWalletBlock()
        observeNameValidation()
        observeSaveButton()
        loadExistingContact()
        sendAddContactTappedEvent()
    }

    private fun sendAddContactTappedEvent() {
        if (params.contactId != null) return
        analyticsSender.sendAddContactTapped(fromSendSuccess = params.predefinedAddress != null, scope = modelScope)
    }

    // region Initialization

    private fun updateInitialState() {
        stateController.update(
            UpdateEditContactInitialStateTransformer(
                isExistingContact = params.contactId != null,
                onNameChange = ::onNameChange,
                onColorSelect = ::onColorSelect,
                onCloseClick = ::onCloseClick,
                onAddAddressClick = ::onAddAddressClick,
                onAddressClick = ::onAddressClick,
                onSaveClick = ::onSaveClick,
                onDeleteClick = if (params.contactId != null) ::onDeleteClick else null,
            ),
        )
    }

    /** In WithContactCreation mode the contact opens with the already-known address attached. */
    private fun prefillPredefinedAddress() {
        params.predefinedAddress?.let(::addAddress)
    }

    /** Loads an existing contact and prefills the editor. A new contact needs nothing — its baseline is empty. */
    private fun loadExistingContact() {
        val contactId = params.contactId ?: return
        modelScope.launch {
            val contact = getContactByIdUseCase(contactId).first()
            if (contact == null) {
                params.onBackClick()
                return@launch
            }
            loadedContact.value = contact
            prefillFromContact(contact)
        }
    }

    private fun prefillFromContact(contact: Contact) {
        stateController.update(UpdateContactNameTransformer(name = contact.name.value))
        CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == contact.iconColor }
            ?.let { stateController.update(SelectContactColorTransformer(color = it)) }
        val addresses = ContactAddressEntriesConverter().toValidatedAddresses(contact.addressEntries)
        stateController.update(SetValidatedAddressesTransformer(addresses = addresses, maxAddresses = MAX_ADDRESSES))
    }

    // endregion

    // region Subscriptions

    private fun subscribeToConfirmedAddresses() {
        resultHolder.confirmedAddress
            .filterNotNull()
            .onEach { confirmed ->
                // Edit-address: the result carries the entry it supersedes, so we swap it in place.
                confirmed.replaces?.let { old ->
                    stateController.update(
                        RemoveValidatedAddressTransformer(address = old, maxAddresses = MAX_ADDRESSES),
                    )
                }
                addAddress(confirmed.address)
                resultHolder.clear()
            }
            .launchIn(modelScope)
    }

    /** Closes the wallet selector as soon as the user picks a wallet in it. */
    private fun dismissSelectorOnPick() {
        pickedWallet
            .filterNotNull()
            .onEach { portfolioSelectorNavigation.dismiss() }
            .launchIn(modelScope)
    }

    private fun observeWalletBlock() {
        combine(selectedWallet, userWalletsListRepository.userWallets) { wallet, wallets ->
            UpdateWalletBlockTransformer(
                walletName = wallet?.name.orEmpty(),
                isChangeable = isWalletChangeable(wallets),
                onClick = ::onWalletBlockClick,
            )
        }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeNameValidation() {
        combine(
            stateController.uiState.map { it.name }.distinctUntilChanged().debounce(NAME_DEBOUNCE_MS),
            selectedWallet.mapNotNull { it?.walletId }.distinctUntilChanged(),
        ) { name, walletId -> name to walletId }
            .mapLatest { (name, walletId) -> validateName(name, walletId) }
            .onEach { error -> stateController.update(UpdateNameErrorTransformer(error)) }
            .launchIn(modelScope)
    }

    private fun observeSaveButton() {
        // Recompute on input changes and on wallet changes (the wallet type drives the button's Tangem-logo icon).
        combine(
            stateController.uiState
                .map { state ->
                    SaveButtonInputs(
                        name = state.name,
                        hasNameError = state.nameError != null,
                        hasAddresses = state.addresses.isNotEmpty(),
                    )
                }
                .distinctUntilChanged(),
            selectedWallet.map { it is UserWallet.Cold }.distinctUntilChanged(),
        ) { _, _ -> }
            .onEach { refreshSaveButton() }
            .launchIn(modelScope)
    }

    // endregion

    // region Clicks

    private fun onNameChange(name: String) {
        stateController.update(UpdateContactNameTransformer(name = name))
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        stateController.update(SelectContactColorTransformer(color = color))
    }

    private fun onWalletBlockClick() {
        if (isWalletChangeable(userWalletsListRepository.userWallets.value)) {
            analyticsSender.sendSaveToButtonClicked()
            portfolioSelectorNavigation.activate(Unit)
        }
    }

    private fun onAddAddressClick() {
        if (stateController.uiState.value.addresses.size >= MAX_ADDRESSES) {
            messageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.address_book_max_networks_alert_title),
                    message = resourceReference(R.string.address_book_max_networks_alert_description),
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_ok),
                            onClick = onDismissRequest,
                        )
                    },
                ),
            )
        } else {
            val walletId = selectedWallet.value?.walletId?.stringValue ?: return
            analyticsSender.sendAddressScreenOpened()
            params.onAddAddressClick(walletId, params.contactId?.value, null)
        }
    }

    private fun onSaveClick() {
        if (saveJob?.isActive == true) return
        val userWallet = selectedWallet.value ?: return
        val ui = stateController.uiState.value
        val addressEntries = ContactAddressEntriesConverter().convert(ui.addresses)
        val existing = loadedContact.value

        saveJob = modelScope.launch {
            val result = if (existing != null) {
                saveContactInteractor.updateContact(
                    userWallet = userWallet,
                    contact = existing,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addressEntries = addressEntries,
                )
            } else {
                saveContactInteractor.createContact(
                    userWallet = userWallet,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addressEntries = addressEntries,
                )
            }
            result.fold(
                ifLeft = { error ->
                    handleSaveError(error)
                    analyticsSender.sendSaveErrorShown(
                        walletId = userWallet.walletId,
                        contactId = params.contactId?.value,
                        error = error,
                    )
                    saveJob?.cancel()
                    refreshSaveButton()
                },
                ifRight = { contact ->
                    if (existing == null) {
                        analyticsSender.sendContactSaved(
                            walletId = userWallet.walletId,
                            contactId = contact.id.value,
                            isEdit = params.contactId != null,
                        )
                        messageSender.send(
                            SnackbarMessage(
                                message = resourceReference(R.string.address_book_create_success_message),
                                startIconId = R.drawable.ic_success_20,
                            ),
                        )
                    }
                    params.onBackClick()
                },
            )
        }
        refreshSaveButton()
    }

    private fun onCloseClick() {
        if (isDirty()) showDiscardDialog() else params.onBackClick()
    }

    private fun onDeleteClick() {
        messageSender.send(
            DialogMessage(
                message = resourceReference(R.string.address_book_delete_contact_description),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_delete),
                        isWarning = true,
                        onClick = ::deleteContact,
                    )
                },
            ),
        )
    }

    private fun onAddressClick(address: ValidatedAddress) {
        addressInfoNavigation.activate(address.address)
    }

    fun createAddressInfoParams(address: String): DefaultAddressInfoComponent.Params {
        val entry = stateController.uiState.value.addresses.firstOrNull { it.address == address }
        return DefaultAddressInfoComponent.Params(
            address = address,
            networkCount = entry?.networkIds?.size ?: 0,
            onEditAddress = { onEditAddress(address) },
            onDeleteAddress = { onDeleteAddress(address) },
            onDismiss = { addressInfoNavigation.dismiss() },
        )
    }

    private fun onEditAddress(address: String) {
        val entry = stateController.uiState.value.addresses.firstOrNull { it.address == address } ?: return
        val walletId = selectedWallet.value?.walletId?.stringValue ?: return
        addressInfoNavigation.dismiss()
        params.onAddAddressClick(walletId, params.contactId?.value, entry)
    }

    private fun onDeleteAddress(address: String) {
        addressInfoNavigation.dismiss()
        val isLastAddress = stateController.uiState.value.addresses.size <= 1
        if (isLastAddress && params.contactId != null) {
            onDeleteClick()
        } else {
            stateController.update(RemoveValidatedAddressTransformer(address = address, maxAddresses = MAX_ADDRESSES))
        }
    }

    // endregion

    // region Helpers

    private fun addAddress(address: ValidatedAddress) {
        stateController.update(AddValidatedAddressTransformer(address = address, maxAddresses = MAX_ADDRESSES))
    }

    private fun isWalletChangeable(wallets: List<UserWallet>?): Boolean {
        val unlockedWalletsCount = wallets.orEmpty().count { !it.isLocked }
        return params.contactId == null && unlockedWalletsCount > 1
    }

    private suspend fun validateName(name: String, walletId: UserWalletId): TextReference? {
        if (name.isBlank()) return null
        if (name == loadedContact.value?.name?.value) return null
        val error = validateContactNameUseCase(walletId, name).leftOrNull() ?: return null
        if (error is ContactNameValidationError.Format && error.error is ContactName.Error.Empty) return null
        return ContactNameErrorConverter().convert(error)
    }

    private fun refreshSaveButton() {
        val ui = stateController.uiState.value
        val isSaving = saveJob?.isActive == true
        val isEnabled = ui.name.isNotBlank() && ui.nameError == null && ui.addresses.isNotEmpty() && !isSaving
        stateController.update(
            UpdateSaveButtonTransformer(
                isEnabled = isEnabled,
                isLoading = isSaving,
                isColdWallet = selectedWallet.value is UserWallet.Cold,
            ),
        )
    }

    private fun handleSaveError(error: SaveContactError) {
        when (error) {
            is SaveContactError.Name -> stateController.update(
                UpdateNameErrorTransformer(ContactNameErrorConverter().convert(error.error)),
            )
            else -> messageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.common_something_went_wrong),
                    message = resourceReference(R.string.address_book_creating_error),
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_ok),
                            onClick = onDismissRequest,
                        )
                    },
                ),
            )
        }
    }

    private fun deleteContact() {
        val contactId = params.contactId ?: return
        modelScope.launch {
            deleteContactUseCase(contactId).fold(
                ifLeft = { showDeleteError() },
                ifRight = { params.onBackClick() },
            )
        }
    }

    private fun showDiscardDialog() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.address_book_unsaved_changes),
                message = resourceReference(R.string.address_book_unsaved_changes_description),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.address_book_keep_editing),
                        onClick = onDismissRequest,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.address_book_discard),
                        isWarning = true,
                        onClick = params.onBackClick,
                    )
                },
            ),
        )
    }

    private fun showDeleteError() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.address_book_deleting_error),
            ),
        )
    }

    /** Dirty when the current editor differs from its baseline — the loaded contact, or the empty new contact. */
    private fun isDirty(): Boolean {
        val baseline = loadedContact.value?.toSnapshot() ?: newContactBaseline
        return currentSnapshot() != baseline
    }

    private fun currentSnapshot(): EditSnapshot {
        val ui = stateController.uiState.value
        return EditSnapshot(
            name = ui.name,
            colorName = ui.colors.selected.name,
            addresses = ui.addresses.map { it.address to it.networkIds.toSet() },
        )
    }

    private fun Contact.toSnapshot(): EditSnapshot = EditSnapshot(
        name = name.value,
        colorName = iconColor,
        addresses = ContactAddressEntriesConverter().toValidatedAddresses(addressEntries)
            .map { it.address to it.networkIds.toSet() },
    )

    // endregion

    // region Models

    private data class SaveButtonInputs(
        val name: String,
        val hasNameError: Boolean,
        val hasAddresses: Boolean,
    )

    private data class EditSnapshot(
        val name: String,
        val colorName: String,
        val addresses: List<Pair<String, Set<String>>>,
    )

    // endregion

    private companion object {
        const val MAX_ADDRESSES = 20
        const val NAME_DEBOUNCE_MS = 300L
    }
}