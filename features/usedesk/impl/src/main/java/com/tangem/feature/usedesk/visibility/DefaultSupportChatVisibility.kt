package com.tangem.feature.usedesk.visibility

import com.tangem.feature.usedesk.api.SupportChatVisibilityProvider
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSupportChatVisibility @Inject constructor() : SupportChatVisibilityProvider {

    private val resumedChats = AtomicInteger(0)

    override val isChatVisible: Boolean
        get() = resumedChats.get() > 0

    fun onResumed() {
        resumedChats.incrementAndGet()
    }

    fun onPaused() {
        resumedChats.updateAndGet { (it - 1).coerceAtLeast(0) }
    }
}