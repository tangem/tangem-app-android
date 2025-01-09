package com.tangem.blockchainsdk.providers

import com.google.common.truth.Truth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.datasource.local.config.providers.models.ProviderModel
import io.mockk.*
import org.junit.Before
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class BlockchainProvidersResponseMergerTest {

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        val firebaseCrashlytics = mockk<FirebaseCrashlytics>()
        every { FirebaseCrashlytics.getInstance() } returns firebaseCrashlytics
        every { firebaseCrashlytics.recordException(any()) } just Runs
    }

    @Test
    fun test_if_both_configs_are_empty() {
        val expected = emptyMap<String, List<ProviderModel>>()

        val actual = BlockchainProvidersResponseMerger.merge(
            local = emptyMap(),
            remote = emptyMap(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_local_config_is_empty() {
        val expected = remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = emptyMap(),
            remote = remoteResponse,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_is_empty() {
        val expected = localResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = emptyMap(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_both_configs_are_not_empty() {
        val expected = remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = remoteResponse,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_configs_are_equal() {
        val expected = remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = remoteResponse,
            remote = remoteResponse,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_local_config_has_blockchain_with_empty_providers() {
        val eth = "ethereum" to emptyList<ProviderModel>()
        val localResponseWithEth = localResponse + eth

        val expected = localResponseWithEth + remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponseWithEth,
            remote = remoteResponse,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_has_blockchain_with_empty_providers() {
        val expected = remoteResponse

        val eth = "ethereum" to emptyList<ProviderModel>()
        val remoteWithEth = remoteResponse + eth

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = remoteWithEth,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_doesnt_contain_local_providers() {
        val remoteWithoutLocal = remoteResponse - localResponse.keys

        /**
         * The expected result does not contain local blockchains, since they can be disabled remotely.
         * For the opposite case, see [test_if_both_configs_are_not_empty].
         */
        val expected = remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = remoteWithoutLocal,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_contains_unsupported_providers() {
        val nowNodesProvider = ProviderModel.Private(name = "nownodes")
        val eth = "ethereum" to listOf(ProviderModel.UnsupportedType, nowNodesProvider)
        val remoteWithEth = remoteResponse + eth

        val expected = remoteResponse + ("ethereum" to listOf(nowNodesProvider))

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = remoteWithEth,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_remote_config_contains_invalid_public_providers() {
        val eth = "ethereum" to listOf(ProviderModel.UnsupportedType, ProviderModel.Public("adbw2138"))
        val remoteWithEth = remoteResponse + eth

        val expected = remoteResponse

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localResponse,
            remote = remoteWithEth,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun test_if_configs_contain_public_providers_without_slash_in_the_end() {
        val eth = "ethereum" to listOf(ProviderModel.Public("https://qwe.com"))
        val kaspa = "kaspa" to listOf(ProviderModel.Public("https://qwe.com"))

        val localWithKaspa = localResponse + kaspa
        val remoteWithEth = remoteResponse + eth

        val expected = remoteResponse + eth.addSlash() + kaspa.addSlash()

        val actual = BlockchainProvidersResponseMerger.merge(
            local = localWithKaspa,
            remote = remoteWithEth,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    companion object {

        val localResponse: BlockchainProvidersResponse = mapOf(
            "aptos" to listOf(ProviderModel.Private(name = "nownodes")),
            "algorand" to listOf(
                ProviderModel.Private(name = "nownodes"),
                ProviderModel.Public(url = "https://public_alg.com/"),
            ),
        )

        // local + bitcoin
        val remoteResponse: BlockchainProvidersResponse = mapOf(
            "aptos" to listOf(ProviderModel.Private(name = "nownodes")),
            "algorand" to listOf(
                ProviderModel.Private(name = "nownodes"),
                ProviderModel.Public(url = "https://public_alg.com/"),
            ),
            "bitcoin" to listOf(ProviderModel.Private(name = "blockchair")),
        )

        private fun Pair<String, List<ProviderModel.Public>>.addSlash(): Pair<String, List<ProviderModel.Public>> {
            return first to second.map { it.copy(url = "${it.url}/") }
        }
    }
}