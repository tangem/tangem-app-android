package com.tangem.data.settings

import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
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
class DevHotWalletRestrictionManagerTest {

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
    fun `GIVEN preference is true WHEN isCreationEnabled THEN emits true`() = runTest {
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Boolean>>(), default = true)
        } returns flowOf(true)

        val manager = DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)

        val emitted = getEmittedValues(manager.isCreationEnabled())

        assertThat(emitted).containsExactly(true)
    }

    @Test
    fun `GIVEN preference is false WHEN isCreationEnabled THEN emits false`() = runTest {
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Boolean>>(), default = true)
        } returns flowOf(false)

        val manager = DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)

        val emitted = getEmittedValues(manager.isCreationEnabled())

        assertThat(emitted).containsExactly(false)
    }

    @Test
    fun `GIVEN preference Flow emits true WHEN isCreationEnabledSync THEN returns cached true`() = runTest {
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Boolean>>(), default = true)
        } returns flowOf(true)

        val manager = DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)

        assertThat(manager.isCreationEnabledSync()).isTrue()
    }

    @Test
    fun `GIVEN preference Flow emits false WHEN isCreationEnabledSync THEN returns cached false`() = runTest {
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Boolean>>(), default = true)
        } returns flowOf(false)

        val manager = DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)

        assertThat(manager.isCreationEnabledSync()).isFalse()
    }

    @Test
    fun `WHEN toggleCreationEnabled THEN opens editData transaction`() = runTest {
        every {
            appPreferencesStore.get(key = any<Preferences.Key<Boolean>>(), default = true)
        } returns flowOf(true)
        coEvery { appPreferencesStore.editData(any()) } returns mockk(relaxed = true)

        val manager = DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)
        manager.toggleCreationEnabled()

        coVerify { appPreferencesStore.editData(any()) }
    }
}