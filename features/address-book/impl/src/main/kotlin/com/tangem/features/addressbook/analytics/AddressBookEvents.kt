package com.tangem.features.addressbook.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.domain.models.wallet.UserWalletId

private const val ADDRESS_BOOK_CATEGORY = "Address Book"

private const val WALLET_ID = "Wallet Id"
private const val CONTACT_ID = "Contact Id"
private const val MODE = "Mode"

sealed class AddressBookEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(ADDRESS_BOOK_CATEGORY, event, params) {

    // region Contact creation
    data object SaveToButtonClicked : AddressBookEvents(event = "Button - Save To")

    data object AddressScreenOpened : AddressBookEvents(event = "Address Screen Opened")

    class ContactListScreenOpened(
        walletId: UserWalletId,
        source: Source,
    ) : AddressBookEvents(
        event = "Contact List Screen Opened",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            Settings("settings"),
            SendFlow("send_flow"),
        }
    }

    class AddContactTapped(
        walletId: UserWalletId,
        source: Source,
    ) : AddressBookEvents(
        event = "Add Contact Tapped",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            Settings("settings"),
            SendSuccess("send_success"),
        }
    }

    class ContactSaved(
        walletId: UserWalletId,
        contactId: String,
        mode: Mode,
    ) : AddressBookEvents(
        event = "Contact Saved",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
            MODE to mode.value,
        ),
    ) {
        enum class Mode(val value: String) {
            Create("create"),
            Edit("edit"),
        }
    }

    class SaveErrorShown(
        walletId: UserWalletId,
        contactId: String?,
        errorType: ErrorType,
    ) : AddressBookEvents(
        event = "Save Error Shown",
        params = buildMap {
            put(WALLET_ID, walletId.stringValue)
            contactId?.let { put(CONTACT_ID, it) }
            put(ERROR_TYPE, errorType.value)
        },
    ) {
        enum class ErrorType(val value: String) {
            Network("network"),
            Server("server"),
            Signing("signing"),
        }
    }
    // endregion

    // region Contact editing
    class ContactOpened(
        walletId: UserWalletId,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Opened",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
        ),
    )
    // endregion

    // region Send flow
    class SendFlowWidgetShown(
        walletId: UserWalletId,
    ) : AddressBookEvents(
        event = "Send Flow Widget Shown",
        params = mapOf(WALLET_ID to walletId.stringValue),
    )

    class ContactSelectedInSend(
        walletId: UserWalletId,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Selected In Send",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
        ),
    )

    class AddressSubstitutedInSend(
        walletId: UserWalletId,
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Substituted In Send",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
        ),
    )
    // endregion

    // region Input errors
    class AddressInvalid(
        walletId: UserWalletId,
        contactId: String?,
    ) : AddressBookEvents(
        event = "Address Invalid",
        params = buildMap {
            put(WALLET_ID, walletId.stringValue)
            put(CONTACT_ID, contactId.orEmpty())
        },
    )

    class DuplicateNameErrorShown(
        walletId: UserWalletId,
        contactId: String?,
    ) : AddressBookEvents(
        event = "Duplicate Name Error Shown",
        params = buildMap {
            put(WALLET_ID, walletId.stringValue)
            contactId?.let { put(CONTACT_ID, it) }
        },
    )
    // endregion

    // region Deletion
    class AddressRemoved(
        walletId: UserWalletId,
        contactId: String,
    ) : AddressBookEvents(
        event = "Address Removed",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
        ),
    )

    class ContactDeleted(
        walletId: UserWalletId,
        contactId: String,
    ) : AddressBookEvents(
        event = "Contact Deleted",
        params = mapOf(
            WALLET_ID to walletId.stringValue,
            CONTACT_ID to contactId,
        ),
    )
    // endregion
}