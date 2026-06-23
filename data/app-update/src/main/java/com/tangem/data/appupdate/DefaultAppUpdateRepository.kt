package com.tangem.data.appupdate

import arrow.core.Either
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.ApplicationVersionsResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.appupdate.model.AppVersionInfo
import com.tangem.domain.appupdate.model.OptionalUpdateShown
import com.tangem.domain.appupdate.repository.AppUpdateRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultAppUpdateRepository(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppUpdateRepository {

    override suspend fun getCachedAppVersionInfo(): AppVersionInfo? = appPreferencesStore
        .getObjectSyncOrNull<ApplicationVersionsResponse>(PreferencesKeys.CACHED_APP_VERSIONS_KEY)
        ?.toDomain()

    override suspend fun refreshAppVersionInfo(): Either<Throwable, AppVersionInfo> = withContext(dispatchers.io) {
        Either.catch {
            val response = tangemTechApi.getApplicationVersions().getOrThrow()
            appPreferencesStore.storeObject(PreferencesKeys.CACHED_APP_VERSIONS_KEY, response)
            response.toDomain()
        }.onLeft { error -> TangemLogger.e("Unable to fetch application versions", error) }
    }

    override suspend fun getOptionalUpdateShown(): OptionalUpdateShown? {
        val version = appPreferencesStore.getSyncOrNull(PreferencesKeys.LAST_OPTIONAL_UPDATE_SHOWN_VERSION_KEY)
            ?: return null
        val shownAtMillis = appPreferencesStore.getSyncOrNull(PreferencesKeys.LAST_OPTIONAL_UPDATE_SHOWN_AT_KEY)
            ?: return null

        return OptionalUpdateShown(version = version, shownAtMillis = shownAtMillis)
    }

    override suspend fun setOptionalUpdateShown(shown: OptionalUpdateShown) {
        appPreferencesStore.store(PreferencesKeys.LAST_OPTIONAL_UPDATE_SHOWN_VERSION_KEY, shown.version)
        appPreferencesStore.store(PreferencesKeys.LAST_OPTIONAL_UPDATE_SHOWN_AT_KEY, shown.shownAtMillis)
    }

    private fun ApplicationVersionsResponse.toDomain() = AppVersionInfo(
        minSupportedVersion = minSupportedVersion,
        minSupportedOSVersion = minSupportedOSVersion,
        criticalVersion = criticalVersion,
        criticalOSVersion = criticalOSVersion,
        latestVersion = latestVersion,
    )
}