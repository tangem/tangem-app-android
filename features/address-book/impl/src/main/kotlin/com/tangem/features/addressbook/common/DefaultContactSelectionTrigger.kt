package com.tangem.features.addressbook.common

import com.tangem.features.addressbook.ContactSelectionListener
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.SelectedContact
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot delivery of a contact picked on the full address-book list back to the Send flow.
 *
 * Implements both [ContactSelectionTrigger] (the list emits) and [ContactSelectionListener] (Send collects). The flow
 * is no-replay with a 1-item buffer so [trigger] is non-blocking ([tryEmit]) — the picker can always close even if the
 * collector is momentarily absent; nothing is retained for late subscribers, so there is no stale value to clear.
 */
@Singleton
internal class DefaultContactSelectionTrigger @Inject constructor() :
    ContactSelectionTrigger,
    ContactSelectionListener {

    private val mutableResultFlow = MutableSharedFlow<SelectedContact>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val resultFlow: SharedFlow<SelectedContact> = mutableResultFlow

    override fun trigger(contact: SelectedContact) {
        mutableResultFlow.tryEmit(contact)
    }
}