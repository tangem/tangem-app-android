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
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
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

@Suppress("LongParameterList")
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
    private val analyticsSender: AddressBookAnalyticsSender,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : Model() {

    private val params: DefaultEditContactComponent.Params = paramsContainer.require()

    private val selectedWalletId = MutableStateFlow<UserWalletId?>(null)

    /** The in-flight save coroutine — its [Job.isActive] drives both the re-entrancy guard and the button state. */
    private var saveJob: Job? = null

    val state: StateFlow<EditContactUM> get() = stateController.uiState

    val portfolioSelectorNavigation = SlotNavigation<Unit>()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    init {
        updateInitialState()
        prefillPredefinedAddress()
        subscribeToConfirmedAddresses()
        initSelectedWallet()
        observeWalletSelection()
        observeWalletBlock()
        observeNameValidation()
        observeSaveButton()
        sendAddContactTappedEvent()
    }

    private fun sendAddContactTappedEvent() {
        if (params.contactId != null) return
        analyticsSender.sendAddContactTapped(fromSendSuccess = params.predefinedAddress != null, scope = modelScope)
    }

    /** In WithContactCreation mode the contact opens with the already-known address attached. */
    private fun prefillPredefinedAddress() {
        params.predefinedAddress?.let(::addAddress)
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateEditContactInitialStateTransformer(
                isExistingContact = params.contactId != null,
                onNameChange = ::onNameChange,
                onColorSelect = ::onColorSelect,
                onCloseClick = params.onBackClick,
                onAddAddressClick = ::onAddAddressClick,
                onSaveClick = ::onSaveClick,
            ),
        )
    }

    private fun initSelectedWallet() {
        // TODO: For an existing contact the contact's own wallet should be used here once existing-contact
        //  loading is implemented. For now both new and existing contacts default to the selected wallet.
        userWalletsListRepository.selectedUserWallet
            .filterNotNull()
            .onEach { wallet ->
                if (selectedWalletId.value == null) selectedWalletId.value = wallet.walletId
            }
            .launchIn(modelScope)
    }

    /** Maps the account picked in the selector back to its wallet (wallet-only mode picks the main account). */
    private fun observeWalletSelection() {
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .mapNotNull { it?.first?.walletId }
            .onEach { walletId ->
                selectedWalletId.value = walletId
                portfolioSelectorNavigation.dismiss()
            }
            .launchIn(modelScope)
    }

    private fun observeWalletBlock() {
        combine(
            selectedWalletId,
            userWalletsListRepository.userWallets,
        ) { walletId, wallets ->
            UpdateWalletBlockTransformer(
                walletName = wallets?.firstOrNull { it.walletId == walletId }?.name.orEmpty(),
                isChangeable = isWalletChangeable(wallets),
                onClick = ::onWalletBlockClick,
            )
        }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun isWalletChangeable(wallets: List<UserWallet>?): Boolean {
        val unlockedWalletsCount = wallets.orEmpty().count { !it.isLocked }
        return params.contactId == null && unlockedWalletsCount > 1
    }

    private fun onWalletBlockClick() {
        if (isWalletChangeable(userWalletsListRepository.userWallets.value)) {
            analyticsSender.sendSaveToButtonClicked()
            portfolioSelectorNavigation.activate(Unit)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeNameValidation() {
        combine(
            stateController.uiState.map { it.name }.distinctUntilChanged().debounce(NAME_DEBOUNCE_MS),
            selectedWalletId.filterNotNull(),
        ) { name, walletId -> name to walletId }
            .mapLatest { (name, walletId) -> validateName(name, walletId) }
            .onEach { error -> stateController.update(UpdateNameErrorTransformer(error)) }
            .launchIn(modelScope)
    }

    private suspend fun validateName(name: String, walletId: UserWalletId): TextReference? {
        if (name.isBlank()) return null
        val error = validateContactNameUseCase(walletId, name).leftOrNull() ?: return null
        // A blank name must not surface an inline error; the Empty case is treated as "no error".
        if (error is ContactNameValidationError.Format && error.error is ContactName.Error.Empty) return null
        return ContactNameErrorConverter().convert(error)
    }

    private fun observeSaveButton() {
        stateController.uiState
            .map { state ->
                SaveButtonInputs(
                    name = state.name,
                    hasNameError = state.nameError != null,
                    hasAddresses = state.addresses.isNotEmpty(),
                )
            }
            .distinctUntilChanged()
            .onEach { refreshSaveButton() }
            .launchIn(modelScope)
    }

    /** Recomputes the button from the current inputs and whether a save is running ([saveJob] is active). */
    private fun refreshSaveButton() {
        val ui = stateController.uiState.value
        val isSaving = saveJob?.isActive == true
        val isEnabled = ui.name.isNotBlank() && ui.nameError == null && ui.addresses.isNotEmpty() && !isSaving
        stateController.update(UpdateSaveButtonTransformer(isEnabled = isEnabled, isLoading = isSaving))
    }

    private fun onSaveClick() {
        if (saveJob?.isActive == true) return
        val userWallet = userWalletsListRepository.userWallets.value
            ?.firstOrNull { it.walletId == selectedWalletId.value }
            ?: return
        val ui = stateController.uiState.value
        val addressEntries = ContactAddressEntriesConverter().convert(ui.addresses)

        saveJob = modelScope.launch {
            try {
                // TODO: existing-contact update needs the loaded Contact; existing-contact loading is not implemented.
                val result = saveContactInteractor.createContact(
                    userWallet = userWallet,
                    name = ui.name,
                    iconColor = ui.colors.selected.name,
                    addressEntries = addressEntries,
                )
                result.fold(
                    ifLeft = { error ->
                        handleSaveError(error)
                        analyticsSender.sendSaveErrorShown(
                            walletId = userWallet.walletId,
                            contactId = params.contactId?.value,
                            error = error,
                        )
                    },
                    ifRight = { contact ->
                        analyticsSender.sendContactSaved(
                            walletId = userWallet.walletId,
                            contactId = contact.id.value,
                            isEdit = params.contactId != null,
                        )
                        params.onBackClick()
                    },
                )
            } finally {
                refreshSaveButton()
            }
        }
        refreshSaveButton()
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
                ),
            )
        }
    }

    private fun onAddAddressClick() {
        if (stateController.uiState.value.addresses.size >= MAX_ADDRESSES) {
            messageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.address_book_max_networks_alert_title),
                    message = resourceReference(R.string.address_book_max_networks_alert_description),
                ),
            )
        } else {
            analyticsSender.sendAddressScreenOpened()
            params.onAddAddressClick()
        }
    }

    private fun subscribeToConfirmedAddresses() {
        resultHolder.confirmedAddress
            .filterNotNull()
            .onEach { address ->
                addAddress(address)
                resultHolder.clear()
            }
            .launchIn(modelScope)
    }

    private fun onNameChange(name: String) {
        stateController.update(UpdateContactNameTransformer(name = name))
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        stateController.update(SelectContactColorTransformer(color = color))
    }

    private fun addAddress(address: ValidatedAddress) {
        stateController.update(AddValidatedAddressTransformer(address = address, maxAddresses = MAX_ADDRESSES))
    }

    private data class SaveButtonInputs(
        val name: String,
        val hasNameError: Boolean,
        val hasAddresses: Boolean,
    )

    private companion object {
        const val MAX_ADDRESSES = 20
        const val NAME_DEBOUNCE_MS = 300L
    }
}