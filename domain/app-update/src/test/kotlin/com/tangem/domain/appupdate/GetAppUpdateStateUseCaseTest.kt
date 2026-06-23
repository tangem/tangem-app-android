package com.tangem.domain.appupdate

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.model.AppVersionInfo
import com.tangem.domain.appupdate.model.OptionalUpdateShown
import com.tangem.domain.appupdate.repository.AppUpdateRepository
import com.tangem.domain.appupdate.usecase.GetAppUpdateStateUseCase
import com.tangem.utils.info.AppInfoProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetAppUpdateStateUseCaseTest {

    private val repository = mockk<AppUpdateRepository>()
    private val appInfoProvider = mockk<AppInfoProvider>()
    private val useCase = GetAppUpdateStateUseCase(repository, appInfoProvider, currentTimeMillis = { NOW })

    private fun givenCached(appVersion: String = "5.0", osVersion: String = "14", info: AppVersionInfo?) {
        every { appInfoProvider.appVersion } returns appVersion
        every { appInfoProvider.osVersion } returns osVersion
        coEvery { repository.getCachedAppVersionInfo() } returns info
        coEvery { repository.getOptionalUpdateShown() } returns null
        coEvery { repository.setOptionalUpdateShown(any()) } just Runs
    }

    private fun givenRefresh(appVersion: String = "5.0", osVersion: String = "14", info: AppVersionInfo) {
        every { appInfoProvider.appVersion } returns appVersion
        every { appInfoProvider.osVersion } returns osVersion
        coEvery { repository.refreshAppVersionInfo() } returns info.right()
        coEvery { repository.getOptionalUpdateShown() } returns null
        coEvery { repository.setOptionalUpdateShown(any()) } just Runs
    }

    private fun info(
        minSupportedVersion: String? = null,
        minSupportedOSVersion: String? = null,
        criticalVersion: String? = null,
        criticalOSVersion: String? = null,
        latestVersion: String? = null,
    ) = AppVersionInfo(
        minSupportedVersion = minSupportedVersion,
        minSupportedOSVersion = minSupportedOSVersion,
        criticalVersion = criticalVersion,
        criticalOSVersion = criticalOSVersion,
        latestVersion = latestVersion,
    )

    @Test
    fun `GIVEN app at critical version and OS ok WHEN getCached THEN ForceUpdate`() = runTest {
        givenCached(
            appVersion = "5.0",
            osVersion = "14",
            info = info(criticalVersion = "5.0", criticalOSVersion = "10", latestVersion = "5.1"),
        )

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.ForceUpdate)
    }

    @Test
    fun `GIVEN app at critical version and OS too old WHEN getCached THEN Brick`() = runTest {
        givenCached(
            appVersion = "5.0",
            osVersion = "9",
            info = info(criticalVersion = "5.0", criticalOSVersion = "10", latestVersion = "5.1"),
        )

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.Brick)
    }

    @Test
    fun `GIVEN app at min supported and OS ok WHEN getCached THEN ForceUpdate`() = runTest {
        givenCached(
            appVersion = "5.0",
            osVersion = "14",
            info = info(minSupportedVersion = "5.0", minSupportedOSVersion = "10", latestVersion = "5.1"),
        )

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.ForceUpdate)
    }

    @Test
    fun `GIVEN app at min supported and OS too old WHEN getCached THEN OsTooOld`() = runTest {
        givenCached(
            appVersion = "5.0",
            osVersion = "9",
            info = info(minSupportedVersion = "5.0", minSupportedOSVersion = "10", latestVersion = "5.1"),
        )

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OsTooOld)
    }

    @Test
    fun `GIVEN critical above latest WHEN getCached THEN not blocking and degraded to optional`() = runTest {
        givenCached(appVersion = "5.20", info = info(criticalVersion = "9.99", latestVersion = "5.41"))

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OptionalUpdate)
    }

    @Test
    fun `GIVEN min supported above latest WHEN getCached THEN not blocking and degraded to optional`() = runTest {
        givenCached(appVersion = "5.20", info = info(minSupportedVersion = "9.99", latestVersion = "5.41"))

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OptionalUpdate)
    }

    @Test
    fun `GIVEN blocking threshold but no latest WHEN getCached THEN ignored as NoUpdate`() = runTest {
        givenCached(appVersion = "5.0", info = info(criticalVersion = "5.0", criticalOSVersion = "10"))

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
    }

    @Test
    fun `GIVEN app below latest and not shown before WHEN getCached THEN OptionalUpdate is recorded`() = runTest {
        givenCached(appVersion = "5.0", info = info(latestVersion = "5.37"))

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OptionalUpdate)
        coVerify(exactly = 1) {
            repository.setOptionalUpdateShown(OptionalUpdateShown(version = "5.37", shownAtMillis = NOW))
        }
    }

    @Test
    fun `GIVEN app at latest WHEN getCached THEN NoUpdate`() = runTest {
        givenCached(appVersion = "5.37", info = info(latestVersion = "5.37"))

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
    }

    @Test
    fun `GIVEN optional shown for same version within 24h WHEN getCached THEN NoUpdate`() = runTest {
        givenCached(appVersion = "5.0", info = info(latestVersion = "5.37"))
        coEvery { repository.getOptionalUpdateShown() } returns
            OptionalUpdateShown(version = "5.37", shownAtMillis = NOW - DAY_MILLIS + 1)

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
        coVerify(exactly = 0) { repository.setOptionalUpdateShown(any()) }
    }

    @Test
    fun `GIVEN optional shown for same version over 24h ago WHEN getCached THEN OptionalUpdate`() = runTest {
        givenCached(appVersion = "5.0", info = info(latestVersion = "5.37"))
        coEvery { repository.getOptionalUpdateShown() } returns
            OptionalUpdateShown(version = "5.37", shownAtMillis = NOW - DAY_MILLIS - 1)

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OptionalUpdate)
    }

    @Test
    fun `GIVEN optional shown for older version WHEN getCached THEN OptionalUpdate`() = runTest {
        givenCached(appVersion = "5.0", info = info(latestVersion = "5.37"))
        coEvery { repository.getOptionalUpdateShown() } returns
            OptionalUpdateShown(version = "5.36", shownAtMillis = NOW)

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.OptionalUpdate)
    }

    @Test
    fun `GIVEN all thresholds null WHEN getCached THEN NoUpdate`() = runTest {
        givenCached(info = info())

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
    }

    @Test
    fun `GIVEN critical and latest both match WHEN getCached THEN critical wins`() = runTest {
        givenCached(
            appVersion = "3.0",
            osVersion = "14",
            info = info(criticalVersion = "3.0", minSupportedVersion = "3.0", latestVersion = "5.37"),
        )

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.ForceUpdate)
    }

    @Test
    fun `GIVEN no cache WHEN getCached THEN NoUpdate`() = runTest {
        givenCached(info = null)

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
    }

    @Test
    fun `GIVEN refresh returns blocking info WHEN refresh THEN ForceUpdate`() = runTest {
        givenRefresh(appVersion = "5.0", osVersion = "14", info = info(criticalVersion = "5.0", latestVersion = "5.1"))

        assertThat(useCase.refresh()).isEqualTo(AppUpdateState.ForceUpdate)
    }

    @Test
    fun `GIVEN refresh returns optional info WHEN refresh THEN OptionalUpdate without recording`() = runTest {
        givenRefresh(appVersion = "5.0", info = info(latestVersion = "5.37"))

        assertThat(useCase.refresh()).isEqualTo(AppUpdateState.OptionalUpdate)
        coVerify(exactly = 0) { repository.setOptionalUpdateShown(any()) }
    }

    @Test
    fun `GIVEN refresh fails WHEN refresh THEN falls back to cached thresholds`() = runTest {
        every { appInfoProvider.appVersion } returns "5.0"
        every { appInfoProvider.osVersion } returns "14"
        coEvery { repository.refreshAppVersionInfo() } returns IllegalStateException("error").left()
        coEvery { repository.getCachedAppVersionInfo() } returns info(criticalVersion = "5.0", latestVersion = "5.1")

        assertThat(useCase.refresh()).isEqualTo(AppUpdateState.ForceUpdate)
    }

    @Test
    fun `GIVEN repository throws WHEN getCached THEN NoUpdate`() = runTest {
        coEvery { repository.getCachedAppVersionInfo() } throws IllegalStateException("boom")

        assertThat(useCase.getCached()).isEqualTo(AppUpdateState.NoUpdate)
    }

    @Test
    fun `GIVEN repository throws WHEN refresh THEN NoUpdate`() = runTest {
        coEvery { repository.refreshAppVersionInfo() } throws IllegalStateException("boom")

        assertThat(useCase.refresh()).isEqualTo(AppUpdateState.NoUpdate)
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}