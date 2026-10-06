package com.tangem.features.addressbook.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.common.routing.entity.AddressBookOpenMode

private const val ADDRESS_BOOK_CATEGORY = "Address Book"

private const val CONTACT_ID = "Contact Id"
private const val MODE = "Mode"
private const val CONTACTS_COUNT = "Contacts Count"
private const val ACTION = "Action"

sealed class AddressBookEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(ADDRESS_BOOK_CATEGORY, event, params) {

    // region Contact creation
    data object SaveToButtonClicked : AddressBookEvents(event = "Button - Save To")

    data object AddressScreenOpened : AddressBookEvents(event = "Address Screen Opened")

    class ContactListScreenOpened(
        source: Source,
        contactsCount: Int,
    ) : AddressBookEvents(
        event = "Contact List Screen Opened",
        params = mapOf(
            SOURCE to source.value,
            CONTACTS_COUNT to contactsCount.toString(),
        ),
    ) {
        enum class Source(val value: String) {
            Settings("Settings"),
            SendFlow("Send Flow"),
        }
    }

    class AddContactTapped(
        source: Source,
    ) : AddressBookEvents(
        event = "Add Contact Tapped",
        params = mapOf(SOURCE to source.value),
    ) {
        enum class Source(val value: String) {
            Settings("Settings"),
            SendSuccess("Send Flow"),
            TransactionDetails("Transaction Details"),
        }
    }

    /**
     * [source] is the entry point the editor was opened from, mirroring the [AddContactTapped] this event follows.
     * It is absent when the editor was reached by tapping an existing contact in the list, which starts no creation.
     */
    class ContactSaved(
        contactId: String,
        mode: Mode,
        source: AddContactTapped.Source? = null,
    ) : AddressBookEvents(
        event = "Contact Saved",
        params = buildMap {
            put(CONTACT_ID, contactId)
            put(MODE, mode.value)
            source?.let { put(SOURCE, it.value) }
        },
    ) {
        enum class Mode(val value: String) {
            Create("Create"),
            Edit("Edit"),
        }
    }

    class SaveErrorShown(
        contactId: String?,
        errorType: ErrorType,
    ) : AddressBookEvents(
        event = "Save Error Shown",
        params = buildMap {
            contactId?.let { put(CONTACT_ID, it) }
            put(ERROR_TYPE, errorType.value)
        },
    ) {
        enum class ErrorType(val value: String) {
            Network("Network"),
            Server("Server"),
            Signing("Signing"),
        }
    }
    // endregion

    // region Contact editing
    class ContactScreenOpened(
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Screen Opened",
        params = mapOf(CONTACT_ID to contactId),
    )
    // endregion

    // region Send flow
    data object SendFlowWidgetShown : AddressBookEvents(event = "Send Flow Widget Shown")

    class ContactSelectedInSend(
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Selected",
        params = mapOf(CONTACT_ID to contactId),
    )

    class AddressSubstitutedInSend(
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Substituted In Send",
        params = mapOf(CONTACT_ID to contactId),
    )
    // endregion

    // region Input errors
    class AddressInvalid(
        contactId: String?,
    ) : AddressBookEvents(
        event = "Address Invalid",
        params = mapOf(CONTACT_ID to contactId.orEmpty()),
    )

    class DuplicateNameErrorShown(
        contactId: String?,
    ) : AddressBookEvents(
        event = "Duplicate Name Error Shown",
        params = buildMap {
            contactId?.let { put(CONTACT_ID, it) }
        },
    )
    // endregion

    // region Choose network
    class SelectAllNetworksTapped(
        action: Action,
    ) : AddressBookEvents(
        event = "Select All Networks Tapped",
        params = mapOf(ACTION to action.value),
    ) {
        enum class Action(val value: String) {
            SelectAll("Select All"),
            ClearAll("Clear All"),
        }
    }
    // endregion

    // region Deletion
    class AddressRemoved(
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Removed",
        params = mapOf(CONTACT_ID to contactId),
    )

    class ContactDeleted(
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Deleted",
        params = mapOf(CONTACT_ID to contactId),
    )
    // endregion
}

/** The analytics value of the entry point that opened the contact editor with a prefilled address. */
internal fun AddressBookOpenMode.ContactCreationSource.toAnalyticsSource(): AddressBookEvents.AddContactTapped.Source =
    when (this) {
        AddressBookOpenMode.ContactCreationSource.SendSuccess -> AddressBookEvents.AddContactTapped.Source.SendSuccess
        AddressBookOpenMode.ContactCreationSource.TransactionDetails ->
            AddressBookEvents.AddContactTapped.Source.TransactionDetails
    }