package com.tangem.domain.appupdate.repository

import arrow.core.Either
import com.tangem.domain.appupdate.model.AppVersionInfo
import com.tangem.domain.appupdate.model.OptionalUpdateShown

interface AppUpdateRepository {

    /** Last cached version thresholds, or `null` if nothing has been fetched yet. No network. */
    suspend fun getCachedAppVersionInfo(): AppVersionInfo?

    /** Fetches fresh thresholds and, on success, overwrites the cache. */
    suspend fun refreshAppVersionInfo(): Either<Throwable, AppVersionInfo>

    suspend fun getOptionalUpdateShown(): OptionalUpdateShown?

    suspend fun setOptionalUpdateShown(shown: OptionalUpdateShown)
}