package com.tangem.blockchainsdk.loader

import android.annotation.SuppressLint
import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechServiceApi
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.config.models.ProviderModel
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
@SuppressLint("CheckResult")
@OptIn(ExperimentalStdlibApi::class)
internal class BlockchainProvidersResponseLoaderTest {

    private val tangemTechServiceApi = mockk<TangemTechServiceApi>()
    private val authProvider = mockk<AuthProvider>()
    private val assetReader = mockk<AssetReader>()
    private val moshi = mockk<Moshi>()
    private val jsonAdapter = mockk<JsonAdapter<BlockchainProvidersResponse>>()

    // Impossible to mockk AssetLoader because it implement inline functions
    private val assetLoader = AssetLoader(assetReader = assetReader, moshi = moshi)

    private val loader = BlockchainProvidersResponseLoader(
        tangemTechServiceApi = tangemTechServiceApi,
        authProvider = authProvider,
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

        coVerify(inverse = true) {
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(any(), any())
        }

        Truth.assertThat(actual).isEqualTo(null)
    }

    @Test
    fun test_load_if_remote_config_loading_is_loaded_failure() = runTest {
        everyGettingLocalConfig(json = localProvidersJson) returns localProviders
        everyGettingRemoteConfig() throws IllegalStateException("Test exception")

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
        }

        Truth.assertThat(actual).isEqualTo(localProviders)
    }

    @Test
    fun test_load_if_remote_config_loading_is_loaded_successful() = runTest {
        everyGettingLocalConfig(json = localProvidersJson) returns localProviders
        everyGettingRemoteConfig() returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_remote_config_is_the_same_as_local() = runTest {
        val remoteProviders = localProviders

        everyGettingLocalConfig(json = localProvidersJson) returns localProviders
        everyGettingRemoteConfig() returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_remote_config_has_empty_providers() = runTest {
        val ethProvider = "ethereum" to emptyList<ProviderModel>()
        val remoteProvidersWithEth = remoteProviders + ethProvider

        everyGettingLocalConfig(json = localProvidersJson) returns localProviders
        everyGettingRemoteConfig() returns remoteProvidersWithEth
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
            firebaseCrashlytics.recordException(any())
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    @Test
    fun test_load_if_local_config_has_empty_providers() = runTest {
        val localProvidersWithEmptyApt = localProviders.mapValues { if (it.key == "aptos") emptyList() else it.value }

        everyGettingLocalConfig(json = localProvidersJson) returns localProvidersWithEmptyApt
        everyGettingRemoteConfig() returns remoteProviders

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
        }

        val expected = localProvidersWithEmptyApt + remoteProviders

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_load_if_remote_config_doesnt_contain_local_providers() = runTest {
        val remoteProvidersWithoutLocal: BlockchainProvidersResponse = remoteProviders - localProviders.keys

        everyGettingLocalConfig(json = localProvidersJson) returns localProviders
        everyGettingRemoteConfig() returns remoteProvidersWithoutLocal
        everyCrashlyticsRecording() just Runs

        val actual = loader.load()

        coVerifyOrder {
            assetReader.read(LOCAL_CONFIG_FILE_NAME)
            moshi.adapter<BlockchainProvidersResponse>()
            jsonAdapter.fromJson(localProvidersJson)
            authProvider.getCardPublicKey()
            authProvider.getCardId()
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
            firebaseCrashlytics.recordException(any())
        }

        Truth.assertThat(actual).isEqualTo(remoteProviders)
    }

    private fun everyGettingLocalConfig(
        json: String,
    ): MockKStubScope<BlockchainProvidersResponse?, BlockchainProvidersResponse?> {
        coEvery { assetReader.read(LOCAL_CONFIG_FILE_NAME) } returns json
        every { moshi.adapter<BlockchainProvidersResponse>() } returns jsonAdapter
        return coEvery { jsonAdapter.fromJson(json) }
    }

    private fun everyGettingRemoteConfig(): MockKStubScope<BlockchainProvidersResponse, BlockchainProvidersResponse> {
        every { authProvider.getCardPublicKey() } returns DEFAULT_CARD_PUBLIC_KEY
        every { authProvider.getCardId() } returns DEFAULT_CARD_ID

        return coEvery {
            tangemTechServiceApi.getBlockchainProviders(
                cardPublicKey = DEFAULT_CARD_PUBLIC_KEY,
                cardId = DEFAULT_CARD_ID,
            )
        }
    }

    private fun everyCrashlyticsRecording() = every { firebaseCrashlytics.recordException(any()) }

    private companion object {
        const val LOCAL_CONFIG_FILE_NAME = "tangem-app-config/providers_order.json"
        const val DEFAULT_CARD_PUBLIC_KEY = "card_public_key"
        const val DEFAULT_CARD_ID = "card_id"

        val localProvidersJson = """
            {
                "aptos": [
                    {
                        "type": "private",
                        "name": "private_apt"
                    }
                ],
                "algorand": [
                    {
                        "type": "private",
                        "name": "private_alg"
                    },
                    {
                        "type": "public",
                        "name": "https://public_alg.com"
                    }
                ]
            }
        """.trimIndent()

        val localProviders: BlockchainProvidersResponse = mapOf(
            "aptos" to listOf(ProviderModel.Private(name = "private_apt")),
            "algorand" to listOf(
                ProviderModel.Private(name = "private_alg"),
                ProviderModel.Public(url = "https://public_alg.com"),
            ),
        )

        val remoteProviders: BlockchainProvidersResponse = localProviders + mapOf(
            "bitcoin" to listOf(ProviderModel.Private(name = "private_btc")),
        )
    }
}