package com.tangem.features.onramp.deeplink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

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