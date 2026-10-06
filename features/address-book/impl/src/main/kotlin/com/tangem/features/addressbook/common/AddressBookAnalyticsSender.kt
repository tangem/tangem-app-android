package com.tangem.features.addressbook.common

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.features.addressbook.AddressBookSendAnalytics
import com.tangem.features.addressbook.analytics.AddressBookEvents
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.analytics.AddressBookEvents.SaveErrorShown.ErrorType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressBookAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) : AddressBookSendAnalytics {

    fun sendContactListScreenOpened(source: Source, contactsCount: Int) {
        analyticsEventHandler.send(
            AddressBookEvents.ContactListScreenOpened(source = source, contactsCount = contactsCount),
        )
    }

    fun sendContactScreenOpened(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.ContactScreenOpened(contactId = contactId))
    }

    fun sendSendFlowWidgetShown() {
        analyticsEventHandler.send(AddressBookEvents.SendFlowWidgetShown)
    }

    fun sendContactSelectedInSend(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.ContactSelectedInSend(contactId = contactId))
    }

    override fun onAddressSubstitutedInSend(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.AddressSubstitutedInSend(contactId = contactId))
    }

    fun sendAddressInvalid(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.AddressInvalid(contactId = contactId))
    }

    fun sendDuplicateNameErrorShown(contactId: String?) {
        analyticsEventHandler.send(AddressBookEvents.DuplicateNameErrorShown(contactId = contactId))
    }

    fun sendAddressRemoved(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.AddressRemoved(contactId = contactId))
    }

    fun sendContactDeleted(contactId: String) {
        analyticsEventHandler.send(AddressBookEvents.ContactDeleted(contactId = contactId))
    }

    fun sendSelectAllNetworksTapped(action: AddressBookEvents.SelectAllNetworksTapped.Action) {
        analyticsEventHandler.send(AddressBookEvents.SelectAllNetworksTapped(action = action))
    }

    fun sendAddContactTapped(source: AddressBookEvents.AddContactTapped.Source) {
        analyticsEventHandler.send(AddressBookEvents.AddContactTapped(source = source))
    }

    fun sendContactSaved(
        contactId: String,
        isEdit: Boolean,
        source: AddressBookEvents.AddContactTapped.Source? = null,
    ) {
        analyticsEventHandler.send(
            AddressBookEvents.ContactSaved(
                contactId = contactId,
                mode = if (isEdit) {
                    AddressBookEvents.ContactSaved.Mode.Edit
                } else {
                    AddressBookEvents.ContactSaved.Mode.Create
                },
                source = source,
            ),
        )
    }

    /**
     * Fired when a save failure is surfaced to the user. [contactId] is set only in edit mode. Validation failures
     * ([SaveContactError.Name]) are shown inline rather than as a save error, so they do not produce this event.
     */
    fun sendSaveErrorShown(contactId: String?, error: SaveContactError) {
        val errorType = error.toErrorType() ?: return
        analyticsEventHandler.send(
            AddressBookEvents.SaveErrorShown(contactId = contactId, errorType = errorType),
        )
    }

    private fun SaveContactError.toErrorType(): ErrorType? = when (this) {
        is SaveContactError.Signing -> ErrorType.Signing
        // Network means the backend was unreachable; every other backend outcome (5xx, 412, other codes) is Server.
        is SaveContactError.Backend -> when (error) {
            AddressBookSyncError.Network -> ErrorType.Network
            else -> ErrorType.Server
        }
        is SaveContactError.Name -> null
    }

    fun sendSaveToButtonClicked() {
        analyticsEventHandler.send(AddressBookEvents.SaveToButtonClicked)
    }

    fun sendAddressScreenOpened() {
        analyticsEventHandler.send(AddressBookEvents.AddressScreenOpened)
    }
}