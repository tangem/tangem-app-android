package com.tangem.datasource.asset.loader

import com.google.common.truth.Truth
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.asset.reader.AssetReader
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalStdlibApi::class)
class AssetLoaderTest {

    private val assetReader = mockk<AssetReader>()
    private val moshi = mockk<Moshi>()
    private val assetLoader = AssetLoader(assetReader = assetReader, moshi = moshi)

    @Test
    fun load() = runTest {
        everyReadingJson() returns assetObjectJson

        val jsonAdapter = mockk<JsonAdapter<Asset>>()
        every { moshi.adapter<Asset>() } returns jsonAdapter
        every { jsonAdapter.fromJson(assetObjectJson) } returns assetObject

        val actual = assetLoader.load<Asset>(fileName = JSON_FILE_NAME)

        coVerifyOrder {
            assetReader.read("$JSON_FILE_NAME.json")
            jsonAdapter.fromJson(assetObjectJson)
        }

        Truth.assertThat(actual).isEqualTo(assetObject)
    }

    @Test
    fun loadList() = runTest {
        everyReadingJson() returns assetListJson

        val jsonAdapter = mockk<JsonAdapter<List<Asset>>>()
        val types = Types.newParameterizedType(List::class.java, Asset::class.java)
        every { moshi.adapter<List<Asset>>(types) } returns jsonAdapter
        every { jsonAdapter.fromJson(assetListJson) } returns assetList

        val actual = assetLoader.loadList<Asset>(fileName = JSON_FILE_NAME)

        coVerifyOrder {
            assetReader.read("$JSON_FILE_NAME.json")
            jsonAdapter.fromJson(assetListJson)
        }

        Truth.assertThat(actual).isEqualTo(assetList)
    }

    @Test
    fun loadMap() = runTest {
        everyReadingJson() returns assetMapJson

        val jsonAdapter = mockk<JsonAdapter<Map<String, Asset>>>()
        val types = Types.newParameterizedType(Map::class.java, String::class.java, Asset::class.java)
        every { moshi.adapter<Map<String, Asset>>(types) } returns jsonAdapter
        every { jsonAdapter.fromJson(assetMapJson) } returns assetMap

        val actual = assetLoader.loadMap<Asset>(fileName = JSON_FILE_NAME)

        coVerifyOrder {
            assetReader.read("$JSON_FILE_NAME.json")
            jsonAdapter.fromJson(assetMapJson)
        }

        Truth.assertThat(actual).isEqualTo(assetMap)
    }

    private fun everyReadingJson() = coEvery { assetReader.read("$JSON_FILE_NAME.json") }

    private companion object {

        const val JSON_FILE_NAME = "config"

        val assetObject = Asset(
            contractAddress = "0x0000000000000000000000000000000000000000",
            network = "Network",
            exchangeAvailable = true,
            onrampAvailable = true,
        )

        val assetList = listOf(assetObject, assetObject)

        val assetMap = mapOf("key1" to assetObject, "key2" to assetObject)

        val assetObjectJson = """
            {
              "contractAddress": "${assetObject.contractAddress}",
              "network": "${assetObject.network}",
              "exchangeAvailable": ${assetObject.exchangeAvailable},
              "onrampAvailable": ${assetObject.onrampAvailable}
            }
        """.trimIndent()

        val assetListJson = """
            $assetObjectJson,
            $assetObjectJson
        """.trimIndent()

        val assetMapJson = """
            {
              "key1": $assetObjectJson,
              "key2": $assetObjectJson
            }
        """.trimIndent()
    }
}