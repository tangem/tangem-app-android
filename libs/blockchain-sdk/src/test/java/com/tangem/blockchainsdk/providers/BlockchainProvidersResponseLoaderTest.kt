package com.tangem.blockchainsdk.providers

import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchainsdk.providers.BlockchainProvidersResponseMergerTest.Companion.localResponse
import com.tangem.blockchainsdk.providers.BlockchainProvidersResponseMergerTest.Companion.remoteResponse
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.blockchainsdk.providers.models.ProviderModel
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
[REDACTED_AUTHOR]
 */
internal class BlockchainProvidersResponseLoaderTest {

    private val blockchainProvidersApi = mockk<BlockchainProvidersApi>()
    private val blockchainProvidersStorage = mockk<BlockchainProvidersStorage>()
    private val analyticsExceptionHandler = object : AnalyticsExceptionHandler {
        override fun sendException(event: ExceptionAnalyticsEvent) {
            FirebaseCrashlytics.getInstance().recordException(event.exception)
        }
    }

    private val loader = BlockchainProvidersResponseLoader(
        blockchainProvidersApi = blockchainProvidersApi,
        blockchainProvidersStorage = blockchainProvidersStorage,
        blockchainProvidersResponseMerger = BlockchainProvidersResponseMerger(analyticsExceptionHandler),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
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
        coVerify(inverse = true) { blockchainProvidersApi.getBlockchainProviders() }

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_loading_is_failed() = runTest {
        coEvery { blockchainProvidersStorage.getConfigSync() } returns localResponse
        coEvery { blockchainProvidersApi.getBlockchainProviders() } throws IllegalStateException("Test exception")

        val expected = localResponse

        val actual = loader.load()

        coVerifyOrder {
            blockchainProvidersStorage.getConfigSync()
            blockchainProvidersApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_is_loaded_successfully() = runTest {
        val eth = "ethereum" to listOf(ProviderModel.Private(name = "nownodes"))

        coEvery { blockchainProvidersStorage.getConfigSync() } returns localResponse + eth
        coEvery { blockchainProvidersApi.getBlockchainProviders() } returns remoteResponse

        // Because configs are merged in BlockchainProvidersResponseMerger
        val expected = remoteResponse + eth

        val actual = loader.load()

        coVerifyOrder {
            blockchainProvidersStorage.getConfigSync()
            blockchainProvidersApi.getBlockchainProviders()
        }

        Truth.assertThat(actual).isEqualTo(expected)
    }
}