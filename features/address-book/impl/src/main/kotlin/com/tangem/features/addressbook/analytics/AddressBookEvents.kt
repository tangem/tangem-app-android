package com.tangem.features.addressbook.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE

private const val ADDRESS_BOOK_CATEGORY = "Address Book"

private const val WALLET_ID = "Wallet Id"
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
        walletId: String,
        source: Source,
        contactsCount: Int,
    ) : AddressBookEvents(
        event = "Contact List Screen Opened",
        params = mapOf(
            WALLET_ID to walletId,
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
        walletId: String,
        source: Source,
    ) : AddressBookEvents(
        event = "Add Contact Tapped",
        params = mapOf(
            WALLET_ID to walletId,
            SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            Settings("Settings"),
            SendSuccess("Send Flow"),
        }
    }

    class ContactSaved(
        walletId: String,
        contactId: String,
        mode: Mode,
    ) : AddressBookEvents(
        event = "Contact Saved",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
            MODE to mode.value,
        ),
    ) {
        enum class Mode(val value: String) {
            Create("Create"),
            Edit("Edit"),
        }
    }

    class SaveErrorShown(
        walletId: String,
        contactId: String?,
        errorType: ErrorType,
    ) : AddressBookEvents(
        event = "Save Error Shown",
        params = buildMap {
            put(WALLET_ID, walletId)
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
        walletId: String,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Screen Opened",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
        ),
    )
    // endregion

    // region Send flow
    class SendFlowWidgetShown(
        walletId: String,
    ) : AddressBookEvents(
        event = "Send Flow Widget Shown",
        params = mapOf(WALLET_ID to walletId),
    )

    class ContactSelectedInSend(
        walletId: String,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Selected",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
        ),
    )

    class AddressSubstitutedInSend(
        walletId: String,
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Substituted In Send",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
        ),
    )
    // endregion

    // region Input errors
    class AddressInvalid(
        walletId: String,
        contactId: String?,
    ) : AddressBookEvents(
        event = "Address Invalid",
        params = buildMap {
            put(WALLET_ID, walletId)
            put(CONTACT_ID, contactId.orEmpty())
        },
    )

    class DuplicateNameErrorShown(
        walletId: String,
        contactId: String?,
    ) : AddressBookEvents(
        event = "Duplicate Name Error Shown",
        params = buildMap {
            put(WALLET_ID, walletId)
            contactId?.let { put(CONTACT_ID, it) }
        },
    )
    // endregion

    // region Choose network
    class SelectAllNetworksTapped(
        walletId: String,
        action: Action,
    ) : AddressBookEvents(
        event = "Select All Networks Tapped",
        params = mapOf(
            WALLET_ID to walletId,
            ACTION to action.value,
        ),
    ) {
        enum class Action(val value: String) {
            SelectAll("Select All"),
            ClearAll("Clear All"),
        }
    }
    // endregion

    // region Deletion
    class AddressRemoved(
        walletId: String,
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Removed",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
        ),
    )

    class ContactDeleted(
        walletId: String,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Deleted",
        params = mapOf(
            WALLET_ID to walletId,
            CONTACT_ID to contactId,
        ),
    )
    // endregion
}