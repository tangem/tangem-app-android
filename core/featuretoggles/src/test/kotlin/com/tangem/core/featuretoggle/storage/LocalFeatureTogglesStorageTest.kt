package com.tangem.core.featuretoggle.storage

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.asset.reader.AssetReader
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verifyAll
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
* [REDACTED_AUTHOR]
 */
@SuppressLint("CheckResult")
internal class LocalFeatureTogglesStorageTest {

    private val assetReader = mockk<AssetReader>()
    private val jsonAdapter = mockk<JsonAdapter<List<FeatureToggle>>>()
    private val storage = LocalFeatureTogglesStorage(assetReader, jsonAdapter)

    @Test
    fun `successfully initialize storage`() = runTest {
        coEvery { assetReader.readJson(storage.getConfigPath()) } returns json
        coEvery { jsonAdapter.fromJson(json) } returns featureToggles

        storage.init()

        verifyOrder {
            assetReader.readJson(storage.getConfigPath())
            jsonAdapter.fromJson(json)
        }

        Truth.assertThat(storage.getFeatureToggles()).containsExactlyElementsIn(featureToggles)
    }

    @Test
    fun `failure initialize storage if assetReader throws exception`() = runTest {
        coEvery { assetReader.readJson(storage.getConfigPath()) } returns json
        coEvery { jsonAdapter.fromJson(json) } throws IOException()

        storage.init()

        verifyOrder {
            assetReader.readJson(storage.getConfigPath())
            jsonAdapter.fromJson(json)
        }

        runCatching { storage.getFeatureToggles() }
            .onSuccess { throw IllegalStateException("featureToggles shouldn't be initialized") }
            .onFailure {
                Truth
                    .assertThat(it)
                    .hasMessageThat()
                    .contains("Property featureToggles should be initialized before get.")

                Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java)
            }
    }

    @Test
    fun `failure initialize storage if jsonAdapter throws exception`() = runTest {
        coEvery { assetReader.readJson(storage.getConfigPath()) } throws IOException()

        storage.init()

        verifyOrder { assetReader.readJson(storage.getConfigPath()) }
        verifyAll(inverse = true) { jsonAdapter.fromJson(any<String>()) }

        runCatching { storage.getFeatureToggles() }
            .onSuccess { throw IllegalStateException("featureToggles shouldn't be initialized") }
            .onFailure {
                Truth
                    .assertThat(it)
                    .hasMessageThat()
                    .contains("Property featureToggles should be initialized before get.")

                Truth.assertThat(it).isInstanceOf(IllegalStateException::class.java)
            }
    }

    private companion object {

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
            FeatureToggle(name = "INACTIVE_TEST_FEATURE_ENABLED", version = "undefined"),
            FeatureToggle(name = "ACTIVE2_TEST_FEATURE_ENABLED", version = "1.0.0"),
        )
    }
}
