package com.tangem.domain.appupdate.usecase

import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.model.AppVersion
import com.tangem.domain.appupdate.model.AppVersionInfo
import com.tangem.domain.appupdate.model.OptionalUpdateShown
import com.tangem.domain.appupdate.repository.AppUpdateRepository
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger

class GetAppUpdateStateUseCase(
    private val repository: AppUpdateRepository,
    private val appInfoProvider: AppInfoProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    /**
     * Instant decision computed from the cached thresholds and the current app/OS version. No network.
     * Records the optional update as shown when it decides to show it (24h throttle). Never throws —
     * any failure resolves to [AppUpdateState.NoUpdate] so the initial navigation is never blocked.
     */
    suspend fun getCached(): AppUpdateState = runSuspendCatching {
        resolve(repository.getCachedAppVersionInfo(), recordOptionalShown = true)
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
        val info = repository.refreshAppVersionInfo().getOrNull() ?: repository.getCachedAppVersionInfo()
        resolve(info, recordOptionalShown = false)
    }.getOrElse { error ->
        TangemLogger.e("Unable to resolve app update state", error)
        AppUpdateState.NoUpdate
    }

    private suspend fun resolve(info: AppVersionInfo?, recordOptionalShown: Boolean): AppUpdateState {
        info ?: return AppUpdateState.NoUpdate

        val appVersion = AppVersion.parseOrNull(appInfoProvider.appVersion) ?: return AppUpdateState.NoUpdate
        val deviceOsVersion = AppVersion.parseOrNull(appInfoProvider.osVersion)
        val latestVersion = info.latestVersion?.let(AppVersion::parseOrNull)

        val criticalVersion = info.criticalVersion?.let(AppVersion::parseOrNull)
        if (criticalVersion != null && appVersion <= criticalVersion && isEscapable(latestVersion, criticalVersion)) {
            return blockingStateFor(info.criticalOSVersion, deviceOsVersion, AppUpdateState.Brick)
        }

        val minSupportedVersion = info.minSupportedVersion?.let(AppVersion::parseOrNull)
        if (minSupportedVersion != null &&
            appVersion <= minSupportedVersion &&
            isEscapable(latestVersion, minSupportedVersion)
        ) {
            return blockingStateFor(info.minSupportedOSVersion, deviceOsVersion, AppUpdateState.OsTooOld)
        }

        if (info.latestVersion != null && latestVersion != null && appVersion < latestVersion) {
            return resolveOptionalUpdate(info.latestVersion, recordOptionalShown)
        }

        return AppUpdateState.NoUpdate
    }

    /**
     * A blocking threshold is honored only if the advertised latest version is strictly above it — i.e.
     * updating actually clears the block. A threshold no installable version can satisfy is a backend
     * misconfiguration and is ignored.
     */
    private fun isEscapable(latestVersion: AppVersion?, threshold: AppVersion): Boolean =
        latestVersion != null && latestVersion > threshold

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
    }
}