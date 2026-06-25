package com.tangem.features.addressbook.list.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.SelectedContact
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.list.state.AddressBookListStateController
import com.tangem.features.addressbook.list.state.transformers.UpdateAddressBookListContentTransformer
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Backs the contacts list. The list content is the same however the address book was opened — the open
 * [AddressBookRoute.ListMode] only decides what tapping a contact does:
 *  - [AddressBookRoute.ListMode.Default]: browse / manage contacts (editor is TODO [REDACTED_TASK_KEY]).
 *  - [AddressBookRoute.ListMode.Selector]: pick a recipient for the given network — a single matching address is
 *    returned right away, several open the address selector first.
 */
@Suppress("LongParameterList", "NamedArguments")
@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class AddressBookListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: AddressBookListStateController,
    private val router: Router,
    private val contactSelectionTrigger: ContactSelectionTrigger,
    getVerifiedContactsInteractor: GetVerifiedContactsInteractor,
    getWalletsUseCase: GetWalletsUseCase,
) : Model() {

    private val params = paramsContainer.require<DefaultAddressBookListComponent.Params>()

    val state: StateFlow<AddressBookListUM> get() = stateController.uiState

    /** Address-selector bottom sheet, shown when a picked contact has more than one address in the target network. */
    val selectorNavigation = SlotNavigation<MatchedContact>()

    private val searchQuery = MutableStateFlow(value = "")
    private val searchActive = MutableStateFlow(value = false)
    private val selectedWalletId = MutableStateFlow<String?>(value = null)

    private val allContacts: SharedFlow<List<VerifiedContact>> =
        getVerifiedContactsInteractor(query = "", userWalletId = null)
            .shareIn(modelScope, SharingStarted.Lazily, replay = 1)

    init {
        val matchedContacts = searchQuery.flatMapLatest { query ->
            if (query.isBlank()) allContacts else getVerifiedContactsInteractor(query = query, userWalletId = null)
        }
        combine(
            allContacts,
            matchedContacts,
            searchQuery,
            combine(selectedWalletId, searchActive) { selected, active -> selected to active },
            getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = false, filterLocked = true),
        ) { all, matched, query, (selected, active), wallets ->
            ListInputs(
                allContacts = all,
                matchedContacts = matched,
                query = query,
                selectedWalletId = selected,
                isSearchActive = active,
                wallets = wallets,
            )
        }
            .onEach(::updateState)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun updateState(inputs: ListInputs) {
        stateController.update(
            UpdateAddressBookListContentTransformer(
                allContacts = inputs.allContacts,
                matchedContacts = inputs.matchedContacts,
                mode = params.mode,
                wallets = inputs.wallets,
                selectedWalletId = inputs.selectedWalletId,
                query = inputs.query,
                isSearchActive = inputs.isSearchActive,
                onContactClick = params.onContactClick,
                onPickContact = ::onPickContact,
                onQueryChange = ::onQueryChange,
                onActiveChange = ::onActiveChange,
                onClearQuery = ::onClearQuery,
                onChipSelected = ::onChipSelected,
                onAddContactClick = params.onAddContactClick,
            ),
        )
    }

    private fun onQueryChange(query: String) {
        searchQuery.value = query
    }

    private fun onActiveChange(active: Boolean) {
        searchActive.value = active
    }

    private fun onClearQuery() {
        searchQuery.value = ""
    }

    private fun onChipSelected(walletId: String?) {
        if (selectedWalletId.value == walletId) return
        selectedWalletId.value = walletId
    }

    private fun onPickContact(contact: MatchedContact) {
        val singleEntry = contact.entries.singleOrNull()
        if (singleEntry != null) {
            deliverSelection(contact.toSelectedContact(singleEntry))
        } else {
            selectorNavigation.activate(contact)
        }
    }

    fun deliverSelection(contact: SelectedContact) {
        contactSelectionTrigger.trigger(contact)
        selectorNavigation.dismiss()
        router.pop()
    }

    private data class ListInputs(
        val allContacts: List<VerifiedContact>,
        val matchedContacts: List<VerifiedContact>,
        val query: String,
        val selectedWalletId: String?,
        val isSearchActive: Boolean,
        val wallets: Map<UserWalletId, UserWallet>,
    )
}