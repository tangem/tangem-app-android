package com.tangem.features.addressbook.editcontact.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.usecase.DeleteContactUseCase
import com.tangem.domain.addressbook.usecase.GetContactByIdUseCase
import com.tangem.domain.addressbook.validation.ContactNameValidator
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
    private val contactNameValidator: ContactNameValidator,
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
     * The wallet the contact is saved to. A user pick in the selector always wins (this is how an existing contact
     * is moved to another wallet). With no pick yet it defaults to the existing contact's own wallet, or — for a new
     * contact — the app's currently selected wallet.
     */
    private val selectedWallet: StateFlow<UserWallet?> = combine(
        pickedWallet,
        loadedContact,
        userWalletsListRepository.selectedUserWallet,
        userWalletsListRepository.userWallets,
    ) { picked, contact, currentSelected, wallets ->
        when {
            picked != null -> picked
            contact != null -> wallets?.firstOrNull { it.walletId == contact.walletId }
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
        sendContactScreenOpenedEvent()
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
        val addresses = ContactAddressEntriesConverter().toValidatedAddresses(contact.addresses)
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
            .mapLatest { (name, walletId) -> walletId to validateName(name, walletId) }
            .distinctUntilChanged()
            .onEach { (walletId, error) ->
                if (error == ContactNameValidationError.Duplicate) {
                    analyticsSender.sendDuplicateNameErrorShown(
                        walletId = walletId,
                        contactId = params.contactId?.value,
                    )
                }
                stateController.update(UpdateNameErrorTransformer(error?.let(ContactNameErrorConverter()::convert)))
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun observeSaveButton() {
        combine(
            stateController.uiState
                .map { state ->
                    SaveButtonInputs(
                        name = state.name,
                        hasNameError = state.nameError != null,
                        colorName = state.colors.selected.name,
                        addresses = state.addresses.map { it.address to it.networkIds.toSet() },
                    )
                }
                .distinctUntilChanged(),
            selectedWallet.map { it?.walletId to (it is UserWallet.Cold) }.distinctUntilChanged(),
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
            params.onAddAddressClick(walletId, params.contactId?.value, null)
        }
    }

    private fun onSaveClick() {
        if (saveJob?.isActive == true) return
        val userWallet = selectedWallet.value ?: return
        val ui = stateController.uiState.value
        val addresses = ContactAddressEntriesConverter().convert(ui.addresses)
        val existing = loadedContact.value
        val isWalletChanged = existing != null && existing.walletId != userWallet.walletId

        saveJob = modelScope.launch {
            val result = when {
                existing == null -> saveContactInteractor.createContact(
                    userWallet = userWallet,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addresses = addresses,
                )
                isWalletChanged -> saveContactInteractor.moveContact(
                    targetWallet = userWallet,
                    contact = existing,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addresses = addresses,
                )
                else -> saveContactInteractor.updateContact(
                    userWallet = userWallet,
                    contact = existing,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addresses = addresses,
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
                        messageSender.send(
                            SnackbarMessage(
                                message = resourceReference(R.string.address_book_create_success_message),
                                startIconId = R.drawable.ic_success_20,
                            ),
                        )
                    }
                    analyticsSender.sendContactSaved(
                        walletId = userWallet.walletId,
                        contactId = contact.id.value,
                        isEdit = params.contactId != null,
                    )
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
        showDeleteContactDialog(fromLastAddressRemoval = false)
    }

    private fun showDeleteContactDialog(fromLastAddressRemoval: Boolean) {
        messageSender.send(
            DialogMessage(
                message = resourceReference(R.string.address_book_delete_contact_description),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_delete),
                        isWarning = true,
                        onClick = { deleteContact(fromLastAddressRemoval = fromLastAddressRemoval) },
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
            // Removing the last address deletes the whole contact — analytics is emitted from the confirmed delete.
            showDeleteContactDialog(fromLastAddressRemoval = true)
        } else {
            contactWalletId()?.let { walletId ->
                analyticsSender.sendAddressRemoved(walletId = walletId, contactId = params.contactId?.value.orEmpty())
            }
            stateController.update(RemoveValidatedAddressTransformer(address = address, maxAddresses = MAX_ADDRESSES))
        }
    }

    // endregion

    // region Helpers

    private fun sendContactScreenOpenedEvent() {
        analyticsSender.sendContactScreenOpened(contactId = params.contactId?.value.orEmpty(), scope = modelScope)
    }

    private fun addAddress(address: ValidatedAddress) {
        stateController.update(AddValidatedAddressTransformer(address = address, maxAddresses = MAX_ADDRESSES))
    }

    private fun isWalletChangeable(wallets: List<UserWallet>?): Boolean {
        return wallets.orEmpty().count { !it.isLocked } > 1
    }

    private suspend fun validateName(name: String, walletId: UserWalletId): ContactNameValidationError? {
        if (name.isBlank()) return null
        val loaded = loadedContact.value
        if (loaded != null && name == loaded.name.value && walletId == loaded.walletId) return null
        val error = contactNameValidator.validate(walletId, name).leftOrNull() ?: return null
        if (error is ContactNameValidationError.Format && error.error is ContactName.Error.Empty) return null
        return error
    }

    private fun refreshSaveButton() {
        val ui = stateController.uiState.value
        val isSaving = saveJob?.isActive == true
        val isEnabled = ui.name.isNotBlank() &&
            ui.nameError == null &&
            ui.addresses.isNotEmpty() &&
            isDirty() &&
            !isSaving
        stateController.update(
            UpdateSaveButtonTransformer(
                isEnabled = isEnabled,
                isLoading = isSaving,
                isColdWallet = selectedWallet.value is UserWallet.Cold,
                isNewContact = params.contactId == null,
            ),
        )
    }

    private fun handleSaveError(error: SaveContactError) {
        when {
            error is SaveContactError.Name -> stateController.update(
                UpdateNameErrorTransformer(ContactNameErrorConverter().convert(error.error)),
            )
            error is SaveContactError.Backend && error.error is AddressBookSyncError.VersionMismatch ->
                showUpdateAppDialog()
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
    private fun showUpdateAppDialog() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.force_update_warning_title),
                message = resourceReference(R.string.force_update_warning_message),
            ),
        )
    }

    private fun deleteContact(fromLastAddressRemoval: Boolean) {
        val contactId = params.contactId ?: return
        val walletId = contactWalletId()
        // The address removal precedes the backend delete; the contact-deleted event follows a successful response.
        if (fromLastAddressRemoval && walletId != null) {
            analyticsSender.sendAddressRemoved(walletId = walletId, contactId = contactId.value)
        }
        modelScope.launch {
            deleteContactUseCase(contactId).fold(
                ifLeft = { error ->
                    if (error is AddressBookSyncError.VersionMismatch) showUpdateAppDialog() else showDeleteError()
                },
                ifRight = {
                    if (walletId != null) {
                        analyticsSender.sendContactDeleted(walletId = walletId, contactId = contactId.value)
                    }
                    params.onBackClick()
                },
            )
        }
    }

    private fun contactWalletId(): UserWalletId? = loadedContact.value?.walletId ?: selectedWallet.value?.walletId

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
        val loaded = loadedContact.value ?: return currentSnapshot() != newContactBaseline
        val isWalletChanged = selectedWallet.value?.walletId?.let { it != loaded.walletId } == true
        return isWalletChanged || currentSnapshot() != loaded.toSnapshot()
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
        addresses = ContactAddressEntriesConverter().toValidatedAddresses(addresses)
            .map { it.address to it.networkIds.toSet() },
    )

    // endregion

    // region Models

    private data class SaveButtonInputs(
        val name: String,
        val hasNameError: Boolean,
        val colorName: String,
        val addresses: List<Pair<String, Set<String>>>,
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