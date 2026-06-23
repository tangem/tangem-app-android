package com.tangem.features.addressbook.list.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.SelectedContact
import com.tangem.features.addressbook.common.ContactMatcher
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.list.state.AddressBookListStateController
import com.tangem.features.addressbook.list.state.transformers.UpdateAddressBookListInitialStateTransformer
import com.tangem.features.addressbook.list.state.transformers.UpdateAddressBookListSelectionStateTransformer
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Backs the contacts list. The list content is the same however the address book was opened — the open
 * [AddressBookRoute.ListMode] only decides what tapping a contact does:
 *  - [AddressBookRoute.ListMode.Default]: browse / manage contacts (full UI is TODO [REDACTED_TASK_KEY]).
 *  - [AddressBookRoute.ListMode.Selector]: pick a recipient for the given network — a single matching address is
 *    returned right away, several open the address selector first.
 */
@ModelScoped
internal class AddressBookListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: AddressBookListStateController,
    private val router: Router,
    private val contactSelectionTrigger: ContactSelectionTrigger,
    private val getContactsUseCase: GetContactsUseCase,
) : Model() {

    private val params = paramsContainer.require<DefaultAddressBookListComponent.Params>()

    val state: StateFlow<AddressBookListUM> get() = stateController.uiState

    /** Address-selector bottom sheet, shown when a picked contact has more than one address in the target network. */
    val selectorNavigation = SlotNavigation<MatchedContact>()

    init {
        when (val mode = params.mode) {
            // Browse/manage: full list UI is TODO [REDACTED_TASK_KEY].
            AddressBookRoute.ListMode.Default -> stateController.update(
                UpdateAddressBookListInitialStateTransformer(onAddContactClick = params.onAddContactClick),
            )
            // Pick a recipient: same list, the tap returns the chosen address.
            is AddressBookRoute.ListMode.Selector -> observeSelectionContacts(networkId = mode.networkId)
        }
    }

    private fun observeSelectionContacts(networkId: String) {
        getContactsUseCase(query = "")
            .onEach { contacts ->
                stateController.update(
                    UpdateAddressBookListSelectionStateTransformer(
                        matched = ContactMatcher.match(contacts = contacts, networkId = networkId),
                        onAddContactClick = params.onAddContactClick,
                        onContactClick = ::onPickContact,
                    ),
                )
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
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
}