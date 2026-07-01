package com.tangem.data.settings

import com.tangem.domain.settings.UsedeskTokenTtlManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Production implementation of [UsedeskTokenTtlManager].
 *
 * The Usedesk chat token TTL is fixed to [UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS] in production —
 * a tester override must never leak into release builds.
 * [setTokenTtlMillis] is a no-op since the TTL cannot be changed at runtime.
 */
internal class ProdUsedeskTokenTtlManager : UsedeskTokenTtlManager {

    private val state: StateFlow<Long> = MutableStateFlow(UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS)

    override fun getTokenTtlMillis(): StateFlow<Long> = state
    override fun getTokenTtlMillisSync(): Long = UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS
    override suspend fun setTokenTtlMillis(millis: Long) = Unit
}