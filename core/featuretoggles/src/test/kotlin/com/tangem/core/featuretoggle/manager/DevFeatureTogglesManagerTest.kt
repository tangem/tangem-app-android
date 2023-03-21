package com.tangem.core.featuretoggle.manager

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.squareup.moshi.JsonAdapter
import com.tangem.core.featuretoggle.storage.FeatureToggle
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.version.VersionProvider
import com.tangem.datasource.local.AppPreferenceStorage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifyAll
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Andrew Khokhlov on 15/03/2023
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("CheckResult")
internal class DevFeatureTogglesManagerTest {

    private val localFeatureTogglesStorage = mockk<FeatureTogglesStorage>()
    private val appPreferenceStorage = mockk<AppPreferenceStorage>(relaxed = true)
    private val jsonAdapter = mockk<JsonAdapter<Map<String, Boolean>>>()
    private val versionProvider = mockk<VersionProvider>()
    private val manager = DevFeatureTogglesManager(
        localFeatureTogglesStorage = localFeatureTogglesStorage,
        appPreferenceStorage = appPreferenceStorage,
        jsonAdapter = jsonAdapter,
        versionProvider = versionProvider,
    )

    @Test
    fun `successfully initialize storage if shared prefs kept feature toggles`() = runTest {
        val currentVersion = "1.0.0"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery { appPreferenceStorage.featureToggles } returns savedFeatureToggles
        coEvery { jsonAdapter.fromJson(savedFeatureToggles) } returns savedFeatureTogglesMap
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            jsonAdapter.fromJson(savedFeatureToggles)
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
        val currentVersion = "1.0.0"
        val sharedPrefFeatureToggles = "[]"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery { appPreferenceStorage.featureToggles } returns sharedPrefFeatureToggles
        coEvery { jsonAdapter.fromJson(sharedPrefFeatureToggles) } returns savedFeatureTogglesMap
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            jsonAdapter.fromJson(sharedPrefFeatureToggles)
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
        val currentVersion = "1.0.0"

        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery { appPreferenceStorage.featureToggles } returns ""
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            versionProvider.get()
        }
        verifyAll(inverse = true) { jsonAdapter.fromJson(any<String>()) }

        val expected = localFeatureToggles
            .associateToggles(currentVersion)
            .mapValues(Map.Entry<String, Boolean>::value)

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `successfully initialize storage if versionProvider returns null`() = runTest {
        coEvery { localFeatureTogglesStorage.init() } just Runs
        coEvery { appPreferenceStorage.featureToggles } returns savedFeatureToggles
        coEvery { jsonAdapter.fromJson(savedFeatureToggles) } returns savedFeatureTogglesMap
        coEvery { localFeatureTogglesStorage.featureToggles } returns localFeatureToggles
        coEvery { versionProvider.get() } returns null

        manager.init()

        coVerifyOrder {
            localFeatureTogglesStorage.init()
            jsonAdapter.fromJson(savedFeatureToggles)
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
    fun `change toggle that contains in map`() {
        val changeableToggleName = "INACTIVE_TEST_FEATURE_ENABLED"
        val resultMap = mutableMapOf(
            changeableToggleName to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        manager.setFeatureToggles(resultMap)
        coEvery { jsonAdapter.toJson(resultMap) } returns ""

        manager.changeToggle(changeableToggleName, true)

        resultMap[changeableToggleName] = true
        verifyOrder { jsonAdapter.toJson(resultMap) }

        Truth.assertThat(manager.getFeatureToggles()).containsExactlyEntriesIn(resultMap)
    }

    @Test
    fun `change toggle that doesn't contains in map`() {
        val resultMap = mutableMapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )

        manager.setFeatureToggles(resultMap)

        manager.changeToggle("FEATURE_TOGGLE", true)

        verifyAll(inverse = true) { jsonAdapter.toJson(any()) }

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
