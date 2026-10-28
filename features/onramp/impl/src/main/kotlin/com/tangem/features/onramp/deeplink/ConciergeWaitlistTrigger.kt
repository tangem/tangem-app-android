package com.tangem.features.onramp.deeplink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits when `tangem://onramp?type=concierge_waitlist` arrives. Only a started Buy screen collects it, so the link is
 * dropped when Buy isn't on screen.
 */
internal interface ConciergeWaitlistListener {
    val openWaitlistFlow: Flow<Unit>
}

internal interface ConciergeWaitlistTrigger {
    suspend fun triggerOpenWaitlist()
}

@Singleton
internal class DefaultConciergeWaitlistTrigger @Inject constructor() :
    ConciergeWaitlistTrigger,
    ConciergeWaitlistListener {

    override val openWaitlistFlow = MutableSharedFlow<Unit>()

    override suspend fun triggerOpenWaitlist() {
        openWaitlistFlow.emit(Unit)
    }
}