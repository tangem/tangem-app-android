package com.tangem.features.addressbook.common

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.AddressBookSendAnalytics
import com.tangem.features.addressbook.analytics.AddressBookEvents
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.analytics.AddressBookEvents.SaveErrorShown.ErrorType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressBookAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : AddressBookSendAnalytics {

    fun sendContactListScreenOpened(source: Source, contactsCount: Int, scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(
                AddressBookEvents.ContactListScreenOpened(
                    walletId = selectedWalletId(),
                    source = source,
                    contactsCount = contactsCount,
                ),
            )
        }
    }

    fun sendContactScreenOpened(contactId: String, scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(
                AddressBookEvents.ContactScreenOpened(walletId = selectedWalletId(), contactId = contactId),
            )
        }
    }

    fun sendSendFlowWidgetShown(scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(AddressBookEvents.SendFlowWidgetShown(walletId = selectedWalletId()))
        }
    }

    fun sendContactSelectedInSend(contactId: String, scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(
                AddressBookEvents.ContactSelectedInSend(walletId = selectedWalletId(), contactId = contactId),
            )
        }
    }

    override fun onAddressSubstitutedInSend(walletId: UserWalletId, contactId: String) {
        analyticsEventHandler.send(
            AddressBookEvents.AddressSubstitutedInSend(walletId = walletId.hashForAnalytics(), contactId = contactId),
        )
    }

    fun sendAddressInvalid(walletId: UserWalletId, contactId: String) {
        analyticsEventHandler.send(
            AddressBookEvents.AddressInvalid(walletId = walletId.hashForAnalytics(), contactId = contactId),
        )
    }

    fun sendDuplicateNameErrorShown(walletId: UserWalletId, contactId: String?) {
        analyticsEventHandler.send(
            AddressBookEvents.DuplicateNameErrorShown(walletId = walletId.hashForAnalytics(), contactId = contactId),
        )
    }

    fun sendAddressRemoved(walletId: UserWalletId, contactId: String) {
        analyticsEventHandler.send(
            AddressBookEvents.AddressRemoved(walletId = walletId.hashForAnalytics(), contactId = contactId),
        )
    }

    fun sendContactDeleted(walletId: UserWalletId, contactId: String) {
        analyticsEventHandler.send(
            AddressBookEvents.ContactDeleted(walletId = walletId.hashForAnalytics(), contactId = contactId),
        )
    }

    fun sendSelectAllNetworksTapped(action: AddressBookEvents.SelectAllNetworksTapped.Action, scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(
                AddressBookEvents.SelectAllNetworksTapped(walletId = selectedWalletId(), action = action),
            )
        }
    }

    fun sendAddContactTapped(fromSendSuccess: Boolean, scope: CoroutineScope) {
        scope.launch(dispatcherProvider.default) {
            analyticsEventHandler.send(
                AddressBookEvents.AddContactTapped(
                    walletId = selectedWalletId(),
                    source = if (fromSendSuccess) {
                        AddressBookEvents.AddContactTapped.Source.SendSuccess
                    } else {
                        AddressBookEvents.AddContactTapped.Source.Settings
                    },
                ),
            )
        }
    }

    fun sendContactSaved(walletId: UserWalletId, contactId: String, isEdit: Boolean) {
        analyticsEventHandler.send(
            AddressBookEvents.ContactSaved(
                walletId = walletId.hashForAnalytics(),
                contactId = contactId,
                mode = if (isEdit) {
                    AddressBookEvents.ContactSaved.Mode.Edit
                } else {
                    AddressBookEvents.ContactSaved.Mode.Create
                },
            ),
        )
    }

    /**
     * Fired when a save failure is surfaced to the user. [contactId] is set only in edit mode. Validation failures
     * ([SaveContactError.Name]) are shown inline rather than as a save error, so they do not produce this event.
     */
    fun sendSaveErrorShown(walletId: UserWalletId, contactId: String?, error: SaveContactError) {
        val errorType = error.toErrorType() ?: return
        analyticsEventHandler.send(
            AddressBookEvents.SaveErrorShown(
                walletId = walletId.hashForAnalytics(),
                contactId = contactId,
                errorType = errorType,
            ),
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

    private suspend fun selectedWalletId(): String = userWalletsListRepository.selectedUserWallet
        .filterNotNull()
        .first()
        .walletId
        .hashForAnalytics()

    /** Analytics must never receive a raw wallet id — send its SHA-256 (uppercase hex) instead. */
    private fun UserWalletId.hashForAnalytics(): String = value.calculateSha256().toHexString()
}