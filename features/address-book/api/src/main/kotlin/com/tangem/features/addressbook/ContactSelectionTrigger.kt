package com.tangem.features.addressbook

import kotlinx.coroutines.flow.SharedFlow

/**
 * Delivers a contact picked in the full address-book list (opened in selection mode) back to whatever feature
 * requested the selection. The picker and the requesting feature live in independent model scopes, so a one-shot
 * [SharedFlow] is used instead of a retained holder: nothing is kept after emission, so there is nothing to clear.
 *
 * Mirrors the `SwapChooseTokenNetworkTrigger`/`Listener` pattern.
 */
interface ContactSelectionTrigger {

    fun trigger(contact: SelectedContact)
}

interface ContactSelectionListener {

    val resultFlow: SharedFlow<SelectedContact>
}