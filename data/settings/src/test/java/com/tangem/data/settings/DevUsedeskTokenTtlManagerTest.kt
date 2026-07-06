package com.tangem.data.settings

import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.settings.UsedeskTokenTtlManager
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DevUsedeskTokenTtlManagerTest {

    private val appPreferencesStore = mockk<AppPreferencesStore>(relaxed = true)
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun setup() {
        mockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
    }

    @Test
    fun `GIVEN stored ttl WHEN getTokenTtlMillis THEN emits stored value`() = runTest {
        // Arrange
        val storedTtl = 15L * 60 * 1000
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Long>>(), default = DEFAULT)
        } returns flowOf(storedTtl)

        val manager = DevUsedeskTokenTtlManager(appPreferencesStore, dispatchers)

        // Act
        val emitted = getEmittedValues(manager.getTokenTtlMillis())

        // Assert
        assertThat(emitted).containsExactly(storedTtl)
    }

    @Test
    fun `GIVEN no stored ttl WHEN getTokenTtlMillisSync THEN returns default`() = runTest {
        // Arrange
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Long>>(), default = DEFAULT)
        } returns flowOf(DEFAULT)

        val manager = DevUsedeskTokenTtlManager(appPreferencesStore, dispatchers)

        // Act & Assert
        assertThat(manager.getTokenTtlMillisSync()).isEqualTo(DEFAULT)
    }

    @Test
    fun `GIVEN stored ttl WHEN getTokenTtlMillisSync THEN returns cached value`() = runTest {
        // Arrange
        val storedTtl = 60L * 60 * 1000
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Long>>(), default = DEFAULT)
        } returns flowOf(storedTtl)

        val manager = DevUsedeskTokenTtlManager(appPreferencesStore, dispatchers)

        // Act & Assert
        assertThat(manager.getTokenTtlMillisSync()).isEqualTo(storedTtl)
    }

    @Test
    fun `WHEN setTokenTtlMillis THEN opens editData transaction`() = runTest {
        // Arrange
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Long>>(), default = DEFAULT)
        } returns flowOf(DEFAULT)
        coEvery { appPreferencesStore.editData(any()) } returns mockk(relaxed = true)

        val manager = DevUsedeskTokenTtlManager(appPreferencesStore, dispatchers)

        // Act
        manager.setTokenTtlMillis(millis = 15L * 60 * 1000)

        // Assert
        coVerify { appPreferencesStore.editData(any()) }
    }

    private companion object {
        val DEFAULT = UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS
    }
}