package com.tangem.features.addressbook.common

import com.tangem.common.routing.entity.AddressBookOpenMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.analytics.AddressBookEvents
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.analytics.AddressBookEvents.SaveErrorShown.ErrorType
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
) {

    fun sendContactListScreenOpened(mode: AddressBookOpenMode, scope: CoroutineScope) {
        scope.launch {
            analyticsEventHandler.send(
                AddressBookEvents.ContactListScreenOpened(
                    walletId = selectedWalletId(),
                    source = mode.toAnalyticsSource(),
                ),
            )
        }
    }

    fun sendAddContactTapped(fromSendSuccess: Boolean, scope: CoroutineScope) {
        scope.launch {
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
                walletId = walletId,
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
     * ([SaveContactError.Name]/[SaveContactError.Address]) are shown inline rather than as a save error, so they do
     * not produce this event.
     */
    fun sendSaveErrorShown(walletId: UserWalletId, contactId: String?, error: SaveContactError) {
        val errorType = error.toErrorType() ?: return
        analyticsEventHandler.send(
            AddressBookEvents.SaveErrorShown(
                walletId = walletId,
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
        is SaveContactError.Name,
        is SaveContactError.Address,
        -> null
    }

    fun sendSaveToButtonClicked() {
        analyticsEventHandler.send(AddressBookEvents.SaveToButtonClicked)
    }

    fun sendAddressScreenOpened() {
        analyticsEventHandler.send(AddressBookEvents.AddressScreenOpened)
    }

    private suspend fun selectedWalletId(): UserWalletId = userWalletsListRepository.selectedUserWallet
        .filterNotNull()
        .first()
        .walletId

    private fun AddressBookOpenMode.toAnalyticsSource(): Source = when (this) {
        AddressBookOpenMode.Default -> Source.Settings
        is AddressBookOpenMode.ContactSelection,
        is AddressBookOpenMode.WithContactCreation,
        -> Source.SendFlow
    }
}