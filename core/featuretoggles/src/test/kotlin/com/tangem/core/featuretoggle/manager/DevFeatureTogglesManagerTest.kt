package com.tangem.core.featuretoggle.manager

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.tangem.core.featuretoggle.storage.FeatureToggle
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.collections.set

/**
[REDACTED_AUTHOR]
 */
@SuppressLint("CheckResult")
internal class DevFeatureTogglesManagerTest {

    private val localFeatureTogglesStorage = mockk<FeatureTogglesStorage>()
    private val appPreferenceStore = mockk<AppPreferencesStore>(relaxed = true)
    private val versionProvider = mockk<VersionProvider>()
    private val manager = DevFeatureTogglesManager(
        localFeatureTogglesStorage = localFeatureTogglesStorage,
        appPreferencesStore = appPreferenceStore,
        versionProvider = versionProvider,
    )

    @Test
    fun `successfully initialize storage if shared prefs kept feature toggles`() = runTest {
        val currentVersion = "0.1.0"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery {
            appPreferenceStore.getObjectSyncOrNull<Map<String, Boolean>>(PreferencesKeys.FEATURE_TOGGLES_KEY)
        } returns savedFeatureTogglesMap
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            versionProvider.get()
        }

        val expected = localFeatureToggles
            .associateToggles(currentVersion)
            .mapValues { resultToggle ->
                savedFeatureTogglesMap[resultToggle.key] ?: resultToggle.value
            }

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `successfully initialize storage if shared prefs kept empty list`() = runTest {
        val currentVersion = "0.1.0"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery {
            appPreferenceStore.getObjectSyncOrNull<Map<String, Boolean>>(PreferencesKeys.FEATURE_TOGGLES_KEY)
        } returns emptyMap()
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            versionProvider.get()
        }

        val expected = localFeatureToggles
            .associateToggles(currentVersion)
            .mapValues { resultToggle ->
                savedFeatureTogglesMap[resultToggle.key] ?: resultToggle.value
            }

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `successfully initialize storage if shared prefs didn't keep feature toggles`() = runTest {
        val currentVersion = "0.1.0"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery {
            appPreferenceStore.getObjectSyncOrNull<Map<String, Boolean>>(PreferencesKeys.FEATURE_TOGGLES_KEY)
        } returns null
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            versionProvider.get()
        }

        val expected = localFeatureToggles
            .associateToggles(currentVersion)
            .mapValues(Map.Entry<String, Boolean>::value)

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `successfully initialize storage if versionProvider returns null`() = runTest {
        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery { appPreferenceStore.getSyncOrNull(PreferencesKeys.FEATURE_TOGGLES_KEY) } returns savedFeatureToggles
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns null

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            versionProvider.get()
        }

        val expected = localFeatureToggles
            .associateToggles(currentVersion = "")
            .mapValues { resultToggle ->
                savedFeatureTogglesMap[resultToggle.key] ?: resultToggle.value
            }

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `get feature availability if feature toggle exists`() {
        val featureToggles = mutableMapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to true,
            "ACTIVE2_TEST_FEATURE_ENABLED" to true,
        )
        manager.setFeatureToggles(featureToggles)

        val actual = manager.isFeatureEnabled(name = "INACTIVE_TEST_FEATURE_ENABLED")

        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `get feature availability if feature toggle doesn't exists`() {
        val featureToggles = mutableMapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to true,
        )
        manager.setFeatureToggles(featureToggles)

        val actual = manager.isFeatureEnabled(name = "")

        Truth.assertThat(actual).isFalse()
    }

    @Test
    fun getFeatureToggles() {
        val expected = mutableMapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        manager.setFeatureToggles(expected)

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `change toggle that contains in map`() = runTest {
        val changeableToggleName = "INACTIVE_TEST_FEATURE_ENABLED"
        val resultMap = mutableMapOf(
            changeableToggleName to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        manager.setFeatureToggles(resultMap)

        manager.changeToggle(changeableToggleName, true)

        resultMap[changeableToggleName] = true

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(resultMap)
    }

    @Test
    fun `change toggle that doesn't contains in map`() = runTest {
        val resultMap = mutableMapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        manager.setFeatureToggles(resultMap)

        manager.changeToggle("FEATURE_TOGGLE", true)

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(resultMap)
    }

    private companion object {

        val savedFeatureToggles = """
            [
                {
                    "name": "INACTIVE_TEST_FEATURE_ENABLED",
                    "version": "undefined"
                },
                {
                    "name": "ACTIVE2_TEST_FEATURE_ENABLED",
                    "version": "1.0.0"
                }
            ]
        """.trimIndent()

        val savedFeatureTogglesMap = mapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        val localFeatureToggles = listOf(
            FeatureToggle(name = "INACTIVE_TEST_FEATURE_ENABLED", version = "undefined"),
            FeatureToggle(name = "ACTIVE2_TEST_FEATURE_ENABLED", version = "1.0.0"),
        )
    }
}