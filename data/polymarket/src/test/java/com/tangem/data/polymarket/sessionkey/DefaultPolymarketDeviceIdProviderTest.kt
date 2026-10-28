package com.tangem.data.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketDeviceIdProviderTest {

    @Test
    fun `GIVEN an ANDROID_ID WHEN deviceId THEN returns its salted hash and stores nothing`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val provider = createProvider(androidId = ANDROID_ID, storage = storage)

        // Act
        val actual = provider.deviceId()

        // Assert
        assertThat(actual).isEqualTo(ANDROID_ID_HASH)
        assertThat(storage.entries).isEmpty()
    }

    @Test
    fun `GIVEN the same ANDROID_ID WHEN deviceId after a reinstall THEN the id is unchanged`() = runTest {
        // Arrange
        val beforeReinstall = createProvider(androidId = ANDROID_ID).deviceId()

        // Act
        val actual = createProvider(androidId = ANDROID_ID).deviceId()

        // Assert
        assertThat(actual).isEqualTo(beforeReinstall)
    }

    @Test
    fun `GIVEN no ANDROID_ID WHEN deviceId twice THEN one stored UUID is reused`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage()
        val first = createProvider(androidId = null, storage = storage).deviceId()

        // Act
        val actual = createProvider(androidId = "  ", storage = storage).deviceId()

        // Assert
        assertThat(actual).isEqualTo(first)
        assertThat(actual).matches(UUID_REGEX)
        assertThat(storage.entries.values).containsExactly(first)
    }

    @Test
    fun `GIVEN reading ANDROID_ID throws WHEN deviceId THEN falls back to a UUID`() = runTest {
        // Arrange
        val provider = DefaultPolymarketDeviceIdProvider(
            androidId = { throw SecurityException("denied") },
            appInfoProvider = appInfoProvider(DEVICE),
            secureStorage = InMemorySecureStorage(),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        // Act
        val actual = provider.deviceId()

        // Assert
        assertThat(actual).matches(UUID_REGEX)
    }

    @Test
    fun `GIVEN storage fails WHEN deviceId twice THEN the same in-memory UUID is returned`() = runTest {
        // Arrange
        val storage = InMemorySecureStorage().apply { failWrites = true }
        val provider = createProvider(androidId = null, storage = storage)
        val first = provider.deviceId()

        // Act
        val actual = provider.deviceId()

        // Assert
        assertThat(actual).isEqualTo(first)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the app device name WHEN deviceName THEN returns it trimmed to 64 characters`(model: DeviceNameModel) {
        // Act
        val actual = createProvider(androidId = ANDROID_ID, device = model.device).deviceName

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun createProvider(
        androidId: String?,
        device: String = DEVICE,
        storage: InMemorySecureStorage = InMemorySecureStorage(),
    ) = DefaultPolymarketDeviceIdProvider(
        androidId = { androidId },
        appInfoProvider = appInfoProvider(device),
        secureStorage = storage,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private fun appInfoProvider(device: String): AppInfoProvider = mockk { every { this@mockk.device } returns device }

    private fun provideTestModels() = listOf(
        DeviceNameModel(device = DEVICE, expected = DEVICE),
        DeviceNameModel(device = "a".repeat(70), expected = "a".repeat(64)),
        DeviceNameModel(device = "  samsung SM-S918B  ", expected = "samsung SM-S918B"),
    )

    data class DeviceNameModel(val device: String, val expected: String)

    private companion object {
        const val DEVICE = "Google Pixel 8 Pro"
        const val ANDROID_ID = "9f1c2e3d4b5a6978"
        const val ANDROID_ID_HASH = "b0f83c3a51a9a6162975a75406c7f2ccb90b5771255ec28395adde47e08181e6"
        const val UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    }
}