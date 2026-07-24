package com.tangem.domain.appupdate.usecase

import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.model.AppVersion
import com.tangem.domain.appupdate.model.AppVersionInfo
import com.tangem.domain.appupdate.model.OptionalUpdateShown
import com.tangem.domain.appupdate.repository.AppUpdateRepository
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetAppUpdateStateUseCase(
    private val repository: AppUpdateRepository,
    private val appInfoProvider: AppInfoProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    /**
     * Instant decision computed from the cached thresholds and the current app/OS version. No network.
     * Records the optional update as shown as it decides to show it (24h throttle) — used by the startup gate.
     * Never throws — any failure resolves to [AppUpdateState.NoUpdate] so the initial navigation is never blocked.
     */
    suspend fun getCached(): AppUpdateState = getCachedState(recordOptionalShown = true)

    /**
     * Cache-only update state as a cold [Flow], with the optional-update throttle disabled so a
     * non-dismissible banner stays visible while the optional update is relevant. No network, no side effects.
     */
    fun getBannerStateFlow(): Flow<AppUpdateState> = flow {
        emit(getCachedState(recordOptionalShown = false))
    }

    private suspend fun getCachedState(recordOptionalShown: Boolean): AppUpdateState = runSuspendCatching {
        resolve(freshCachedInfoOrNull(), recordOptionalShown = recordOptionalShown)
    }.getOrElse { error ->
        TangemLogger.e("Unable to resolve cached app update state", error)
        AppUpdateState.NoUpdate
    }

    /**
     * Fetches fresh thresholds (overwriting the cache) and re-evaluates. Falls back to the cache on a
     * network error. Does not record the optional update — used for background and on-screen refreshes.
     * Never throws — any failure resolves to [AppUpdateState.NoUpdate].
     */
    suspend fun refresh(): AppUpdateState = runSuspendCatching {
        val info = repository.refreshAppVersionInfo().getOrNull() ?: freshCachedInfoOrNull()
        resolve(info, recordOptionalShown = false)
    }.getOrElse { error ->
        TangemLogger.e("Unable to resolve app update state", error)
        AppUpdateState.NoUpdate
    }

    /**
     * Cached thresholds, but only while they are still fresh. A cache older than [CACHE_TTL_MILLIS] is
     * ignored so a permanently unreachable backend can't keep the user blocked forever — a successful
     * fetch is required at least once per TTL window to keep a blocking threshold in effect.
     */
    private suspend fun freshCachedInfoOrNull(): AppVersionInfo? {
        val cachedAt = repository.getCachedAppVersionTimestamp() ?: return null
        if (currentTimeMillis() - cachedAt > CACHE_TTL_MILLIS) return null
        return repository.getCachedAppVersionInfo()
    }

    private suspend fun resolve(info: AppVersionInfo?, recordOptionalShown: Boolean): AppUpdateState {
        info ?: return AppUpdateState.NoUpdate

        val appVersion = AppVersion.parseOrNull(appInfoProvider.appVersion) ?: return AppUpdateState.NoUpdate
        val deviceOsVersion = AppVersion.parseOrNull(appInfoProvider.osVersion)
        val latestVersion = info.latestVersion?.let(AppVersion::parseOrNull)

        val criticalVersion = info.criticalVersion?.let(AppVersion::parseOrNull)
        if (criticalVersion != null &&
            appVersion <= criticalVersion &&
            isEscapable(latestVersion, criticalVersion, appVersion)
        ) {
            return blockingStateFor(info.criticalOSVersion, deviceOsVersion, AppUpdateState.Brick)
        }

        val minSupportedVersion = info.minSupportedVersion?.let(AppVersion::parseOrNull)
        if (minSupportedVersion != null &&
            appVersion < minSupportedVersion &&
            isEscapable(latestVersion, minSupportedVersion, appVersion)
        ) {
            return blockingStateFor(info.minSupportedOSVersion, deviceOsVersion, AppUpdateState.OsTooOld)
        }

        if (info.latestVersion != null && latestVersion != null && appVersion < latestVersion) {
            return resolveOptionalUpdate(info.latestVersion, recordOptionalShown)
        }

        return AppUpdateState.NoUpdate
    }

    /**
     * A blocking threshold is honored only if the user can actually escape it by updating. Normally that
     * means the advertised latest version is strictly above the threshold. But a latest version reported
     * below the installed one is stale/misconfigured and must not suppress the block — the store almost
     * certainly has an installable build, so the threshold is honored. A missing latest version leaves no
     * installable target, so the threshold is ignored.
     */
    private fun isEscapable(latestVersion: AppVersion?, threshold: AppVersion, appVersion: AppVersion): Boolean {
        latestVersion ?: return false
        if (latestVersion < appVersion) return true
        return latestVersion > threshold
    }

    private suspend fun resolveOptionalUpdate(latestVersion: String, recordOptionalShown: Boolean): AppUpdateState {
        if (!recordOptionalShown) return AppUpdateState.OptionalUpdate

        val shown = repository.getOptionalUpdateShown()
        val isThrottled = shown != null &&
            shown.version == latestVersion &&
            currentTimeMillis() - shown.shownAtMillis < OPTIONAL_UPDATE_INTERVAL_MILLIS

        if (isThrottled) return AppUpdateState.NoUpdate

        repository.setOptionalUpdateShown(
            OptionalUpdateShown(version = latestVersion, shownAtMillis = currentTimeMillis()),
        )
        return AppUpdateState.OptionalUpdate
    }

    private fun blockingStateFor(
        requiredOsVersion: String?,
        deviceOsVersion: AppVersion?,
        osTooOldState: AppUpdateState,
    ): AppUpdateState {
        val requiredOs = requiredOsVersion?.let(AppVersion::parseOrNull)
        val cannotUpdate = requiredOs != null && deviceOsVersion != null && deviceOsVersion < requiredOs
        return if (cannotUpdate) osTooOldState else AppUpdateState.ForceUpdate
    }

    private companion object {
        const val OPTIONAL_UPDATE_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
        const val CACHE_TTL_MILLIS = 24L * 60 * 60 * 1000
    }
}