package com.tangem.core.configtoggle.storage

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.core.configtoggle.feature.impl.FeatureTogglesConstants
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.asset.reader.AssetReader
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
[REDACTED_AUTHOR]
 */
@SuppressLint("CheckResult")
internal class LocalTogglesStorageTest {

    private val assetReader = mockk<AssetReader>()
    private val moshi = mockk<Moshi>()
    private val jsonAdapter = mockk<JsonAdapter<List<ConfigToggle>>>()

    // Impossible to mockk AssetLoader because it implement inline functions
    private val assetLoader = AssetLoader(assetReader = assetReader, moshi = moshi)

    private val storage = LocalTogglesStorage(assetLoader)

    @Test
    fun `successfully initialize storage`() = runTest {
        everyReadingJson() returns json
        everyCreatingMoshiAdapter() returns jsonAdapter
        everyMappingJson() returns featureToggles

        storage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)

        coVerifyOrder {
            assetReader.read(CONFIG_FILE_NAME)
            jsonAdapter.fromJson(json)
        }

        Truth.assertThat(storage.toggles).containsExactlyElementsIn(featureToggles)
    }

    @Test
    fun `failure initialize storage if assetReader throws exception`() = runTest {
        everyReadingJson() returns json
        everyCreatingMoshiAdapter() returns jsonAdapter
        everyMappingJson() throws IOException()

        storage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)

        coVerifyOrder {
            assetReader.read(CONFIG_FILE_NAME)
            jsonAdapter.fromJson(json)
        }

        Truth.assertThat(storage.toggles).containsExactlyElementsIn(emptyList<ConfigToggle>())
    }

    @Test
    fun `failure initialize storage if jsonAdapter throws exception`() = runTest {
        everyReadingJson() throws IOException()

        storage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)

        coVerifyOrder { assetReader.read(CONFIG_FILE_NAME) }
        verifyAll(inverse = true) { jsonAdapter.fromJson(any<String>()) }

        Truth.assertThat(storage.toggles).containsExactlyElementsIn(emptyList<ConfigToggle>())
    }

    private fun everyReadingJson() = coEvery { assetReader.read(CONFIG_FILE_NAME) }

    private fun everyCreatingMoshiAdapter() = every {
        val types = Types.newParameterizedType(List::class.java, ConfigToggle::class.java)
        moshi.adapter<List<ConfigToggle>>(types)
    }

    private fun everyMappingJson() = every { jsonAdapter.fromJson(json) }

    private companion object {
        const val CONFIG_FILE_NAME = "configs/feature_toggles_config.json"

        val json = """
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

        val featureToggles = listOf(
            ConfigToggle(name = "INACTIVE_TEST_FEATURE_ENABLED", version = "undefined"),
            ConfigToggle(name = "ACTIVE2_TEST_FEATURE_ENABLED", version = "1.0.0"),
        )
    }
}