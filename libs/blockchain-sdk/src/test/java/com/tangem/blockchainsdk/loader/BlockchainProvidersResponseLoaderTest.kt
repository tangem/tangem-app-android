package com.tangem.blockchainsdk.loader

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.config.models.ProviderModel
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
* [REDACTED_AUTHOR]
 */
@SuppressLint("CheckResult")
@OptIn(ExperimentalStdlibApi::class)
internal class BlockchainProvidersResponseLoaderTest {

    private val tangemTechServiceApi = mockk<TangemTechApi>()
    private val assetReader = mockk<AssetReader>()
    private val moshi = mockk<Moshi>()
    private val jsonAdapter = mockk<JsonAdapter<BlockchainProvidersResponse>>()

    // Impossible to mockk AssetLoader because it implement inline functions
    private val assetLoader = AssetLoader(assetReader = assetReader, moshi = moshi)

    private val loader = BlockchainProvidersResponseLoader(
        tangemTechServiceApi = tangemTechServiceApi,
        assetLoader = assetLoader,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val firebaseCrashlytics = mockk<FirebaseCrashlytics>()

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns firebaseCrashlytics
    }

    @Test
    fun test_load_if_local_config_is_empty() = runTest {
        val emptyJson = ""
        everyGettingLocalConfig(json = emptyJson) returns null

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(emptyJson)
        }

        coVerify(inverse = true) { tangemTechServiceApi.getBlockchainProviders() }

        Truth.assertThat(actual).isEqualTo(null)
    }

    @Test
    fun test_load_if_remote_config_loading_is_loaded_failure() = runTest {
        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } throws IllegalStateException("Test exception")

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(localProviders)
    }

    @Test
    fun test_load_if_remote_config_loading_is_loaded_successful() = runTest {
        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_remote_config_is_the_same_as_local() = runTest {
        val remoteProviders = localProviders

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_remote_config_has_empty_providers() = runTest {
        val ethProvider = "ethereum" to emptyList<ProviderModel>()
        val remoteProvidersWithEth = remoteProviders + ethProvider

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProvidersWithEth
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
            firebaseCrashlytics.recordException(any())
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_local_config_has_empty_providers() = runTest {
        val localProvidersWithEmptyApt = localProviders.mapValues { if (it.key == "aptos") emptyList() else it.value }

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProvidersWithEmptyApt
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
        }

        val expected = localProvidersWithEmptyApt + remoteProviders

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_load_if_remote_config_doesnt_contain_local_providers() = runTest {
        val remoteProvidersWithoutLocal: BlockchainProvidersResponse = remoteProviders - localProviders.keys

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProvidersWithoutLocal
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
            firebaseCrashlytics.recordException(any())
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_remote_config_contains_unsupported_types() = runTest {
        val ethProvider = "ethereum" to listOf(ProviderModel.UnsupportedType, ProviderModel.Private(name = "nownodes"))
        val remoteProvidersWithEth = remoteProviders + ethProvider

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProviders
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProvidersWithEth
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
            firebaseCrashlytics.recordException(any())
        }

        val expected = remoteProviders + ("ethereum" to listOf(ProviderModel.Private(name = "nownodes")))

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_load_if_remote_config_contains_invalid_public_providers() = runTest {
        val localEthProvider = "ethereum" to listOf(ProviderModel.Private(name = "nownodes"))
        val localProvidersWithEth = localProviders + localEthProvider

        val remoteEthProvider = "ethereum" to listOf(ProviderModel.UnsupportedType, ProviderModel.Public("adbw2138"))
        val remoteProvidersWithEth = remoteProviders + remoteEthProvider

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProvidersWithEth
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProvidersWithEth
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
            firebaseCrashlytics.recordException(any())
        }

        val expected = remoteProviders + localEthProvider

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_load_if_configs_contain_public_providers_without_slash_in_the_end() = runTest {
        val localPublicProviderUrl = "https://qwe.com"
        val localEthProvider = "ethereum" to listOf(ProviderModel.Public(url = localPublicProviderUrl))
        val localProvidersWithEth = localProviders + localEthProvider

        val remotePublicProviderUrl = "https://rty.com"
        val remoteDogeProvider = "dogecoin" to listOf(ProviderModel.Public(url = remotePublicProviderUrl))
        val remoteProvidersWithDoge = remoteProviders + remoteDogeProvider

        everyGettingLocalConfig(json = LOCAL_PROVIDERS_JSON) returns localProvidersWithEth
        coEvery { tangemTechServiceApi.getBlockchainProviders() } returns remoteProvidersWithDoge
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(LOCAL_PROVIDERS_JSON)
            tangemTechServiceApi.getBlockchainProviders()
            firebaseCrashlytics.recordException(any())
        }

        val expected = remoteProviders +
            localEthProvider.copy(second = listOf(ProviderModel.Public(url = "$localPublicProviderUrl/"))) +
            remoteDogeProvider.copy(second = listOf(ProviderModel.Public(url = "$remotePublicProviderUrl/")))

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun everyGettingLocalConfig(
        json: String,
    ): MockKStubScope<BlockchainProvidersResponse?, BlockchainProvidersResponse?> {
        coEvery { assetReader.read(LOCAL_CONFIG_FILE_NAME) } returns json
        every { moshi.adapter<BlockchainProvidersResponse>() } returns jsonAdapter
        return coEvery { jsonAdapter.fromJson(json) }
    }

    private fun everyCrashlyticsRecording() = every { firebaseCrashlytics.recordException(any()) }

    private companion object {
        const val LOCAL_CONFIG_FILE_NAME = "tangem-app-config/providers_order.json"

        const val LOCAL_PROVIDERS_JSON = "doesn't matter"

        val localProviders: BlockchainProvidersResponse = mapOf(
            "aptos" to listOf(ProviderModel.Private(name = "nownodes")),
            "algorand" to listOf(
                ProviderModel.Private(name = "nownodes"),
                ProviderModel.Public(url = "https://public_alg.com/"),
            ),
        )

        val remoteProviders: BlockchainProvidersResponse = localProviders + mapOf(
            "bitcoin" to listOf(ProviderModel.Private(name = "blockchair")),
        )
    }
}
