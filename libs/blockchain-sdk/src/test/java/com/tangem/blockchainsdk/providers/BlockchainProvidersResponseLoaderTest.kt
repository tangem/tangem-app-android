package com.tangem.blockchainsdk.providers

import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchainsdk.providers.BlockchainProvidersResponseMergerTest.Companion.localResponse
import com.tangem.blockchainsdk.providers.BlockchainProvidersResponseMergerTest.Companion.remoteResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.providers.BlockchainProvidersStorage
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class BlockchainProvidersResponseLoaderTest {

    private val tangemTechApi = mockk<TangemTechApi>()
    private val blockchainProvidersStorage = mockk<BlockchainProvidersStorage>()

    private val loader = BlockchainProvidersResponseLoader(
        tangemTechApi = tangemTechApi,
        blockchainProvidersStorage = blockchainProvidersStorage,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        val firebaseCrashlytics = mockk<FirebaseCrashlytics>()
        every { FirebaseCrashlytics.getInstance() } returns firebaseCrashlytics
        every { firebaseCrashlytics.recordException(any()) } just Runs
    }

    @Test
    fun test_if_local_config_is_empty() = runTest {
        coEvery { blockchainProvidersStorage.getConfigSync() } returns emptyMap()

        val expected = null

        val actual = loader.load()

        coVerifyOrder { blockchainProvidersStorage.getConfigSync() }
        coVerify(inverse = true) { tangemTechApi.getBlockchainProviders() }

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_loading_is_failed() = runTest {
        coEvery { blockchainProvidersStorage.getConfigSync() } returns localResponse
        coEvery { tangemTechApi.getBlockchainProviders() } throws IllegalStateException("Test exception")

        val expected = localResponse

        val actual = loader.load()

        coVerifyOrder {
            blockchainProvidersStorage.getConfigSync()
            tangemTechApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_is_loaded_successfully() = runTest {
        val eth = "ethereum" to listOf(ProviderModel.Private(name = "nownodes"))

        coEvery { blockchainProvidersStorage.getConfigSync() } returns localResponse + eth
        coEvery { tangemTechApi.getBlockchainProviders() } returns remoteResponse

        // Because configs are merged in BlockchainProvidersResponseMerger
        val expected = remoteResponse + eth

        val actual = loader.load()

        coVerifyOrder {
            blockchainProvidersStorage.getConfigSync()
            tangemTechApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(expected)
    }
}