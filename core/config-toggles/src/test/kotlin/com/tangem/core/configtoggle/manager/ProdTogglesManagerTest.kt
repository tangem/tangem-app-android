package com.tangem.core.configtoggle.manager

import android.content.pm.PackageManager
import com.google.common.truth.Truth
import com.tangem.core.configtoggle.feature.impl.FeatureTogglesConstants
import com.tangem.core.configtoggle.feature.impl.ProdFeatureTogglesManager
import com.tangem.core.configtoggle.storage.ConfigToggle
import com.tangem.core.configtoggle.storage.TogglesStorage
import com.tangem.core.configtoggle.utils.associateToggles
import com.tangem.core.configtoggle.version.VersionProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class ProdTogglesManagerTest {

    private val localTogglesStorage = mockk<TogglesStorage>()
    private val versionProvider = mockk<VersionProvider>()
    private val manager = ProdFeatureTogglesManager(localTogglesStorage, versionProvider)

    @Test
    fun `successfully initialize storage`() = runTest {
        val currentVersion = "1.0.0"

        coEvery { localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH) } just Runs
        every { localTogglesStorage.toggles } returns localFeatureToggles
        every { versionProvider.get() } returns currentVersion

        manager.init()

        coVerifyOrder {
            localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)
            versionProvider.get()
        }

        val expected = localFeatureToggles.associateToggles(currentVersion)
        Truth.assertThat(manager.getProdFeatureToggles()).containsExactlyEntriesIn(expected)
    }

    @Test
    fun `successfully initialize storage if versionProvider returns null`() = runTest {
        coEvery { localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH) } just Runs
        every { localTogglesStorage.toggles } returns localFeatureToggles
        every { versionProvider.get() } returns null

        manager.init()

        coVerifyOrder {
            localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)
            versionProvider.get()
        }

        Truth.assertThat(manager.getProdFeatureToggles()).containsExactlyEntriesIn(disabledFeatureToggles)
    }

    @Test
    fun `failure initialize storage if localFeatureTogglesStorage throws exception`() = runTest {
        coEvery { localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH) } just Runs
        every { localTogglesStorage.toggles } throws IllegalStateException(
            "Property featureToggles should be initialized before get.",
        )

        runCatching { manager.init() }
            .onSuccess { throw IllegalStateException("localFeatureToggles shouldn't be initialized") }
            .onFailure {
                Truth
                    .assertThat(it)
                    .hasMessageThat()
                    .contains("Property featureToggles should be initialized before get.")

                Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java)
            }

        coVerifyOrder { localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH) }
        verifyAll(inverse = true) { versionProvider.get() }
    }

    @Test
    fun `failure initialize storage if versionProvider throws exception`() = runTest {
        coEvery { localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH) } just Runs
        every { localTogglesStorage.toggles } returns localFeatureToggles
        every { versionProvider.get() } throws PackageManager.NameNotFoundException()

        runCatching { manager.init() }
            .onSuccess { throw IllegalStateException("versionProvider should throws exception") }
            .onFailure {
                Truth.assertThat(it).isInstanceOf(PackageManager.NameNotFoundException::class.java)
            }

        coVerifyOrder {
            localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)
            versionProvider.get()
        }
    }

    @Test
    fun `get feature availability if feature toggle exists`() {
        val featureToggles = mapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to true,
            "ACTIVE2_TEST_FEATURE_ENABLED" to true,
        )
        manager.setProdFeatureToggles(featureToggles)

        val actual = manager.isFeatureEnabled(name = "INACTIVE_TEST_FEATURE_ENABLED")

        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `get feature availability if feature toggle doesn't exists`() {
        val featureToggles = mapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to true,
        )
        manager.setProdFeatureToggles(featureToggles)

        val actual = manager.isFeatureEnabled(name = "")

        Truth.assertThat(actual).isFalse()
    }

    private companion object {
        val localFeatureToggles = listOf(
            ConfigToggle(name = "INACTIVE_TEST_FEATURE_ENABLED", version = "undefined"),
            ConfigToggle(name = "ACTIVE2_TEST_FEATURE_ENABLED", version = "1.0.0"),
        )

        val disabledFeatureToggles = mapOf(
            "INACTIVE_TEST_FEATURE_ENABLED" to false,
            "ACTIVE2_TEST_FEATURE_ENABLED" to false,
        )
    }
}